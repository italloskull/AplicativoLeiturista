@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.util.Log
import java.util.UUID
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CustomerRepositoryImpl private constructor() : CustomerRepository {
    private val client = SupabaseClient.client
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    override val customers: StateFlow<List<Customer>> = _customers.asStateFlow()
    
    private var applicationContext: android.content.Context? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val refreshMutex = Mutex()

    private var remoteCustomers: List<Customer> = emptyList()
    private var localPendingCustomers: List<Customer> = emptyList()
    private var realtimeChannel: RealtimeChannel? = null

    fun initialize(context: android.content.Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            scope.launch { fetchCustomers() }
        }
    }

    init {
        setupRealtime()
    }

    private fun setupRealtime() {
        scope.launch {
            var retryDelay = 5000L
            while (true) {
                try {
                    realtimeChannel?.unsubscribe()
                    client.realtime.connect()
                    val myChannel = client.realtime.channel("public_clientes")
                    realtimeChannel = myChannel
                    myChannel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "clientes" }.collect {
                        delay(800)
                        fetchCustomers()
                    }
                    myChannel.subscribe()
                    retryDelay = 5000L
                    break
                } catch (e: Exception) {
                    Log.w("debugs", "🛰️ [REALTIME] Falha na conexão. Re-tentando em ${retryDelay/1000}s...")
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(60000L)
                }
            }
        }
    }

    override suspend fun fetchCustomers(cidadeId: String?, isAdmin: Boolean) {
        refreshMutex.withLock {
            try {
                val userCityName = com.example.oaplicativo.util.CityUtils.getFriendlyCityName(cidadeId)
                
                val list = withContext(Dispatchers.IO) {
                    val authRepo = AuthRepositoryImpl.getInstance()
                    val authorizedCities = authRepo.getUserCities()
                    val cityNames = authorizedCities.map { it.nome }

                    client.postgrest["clientes"].select {
                        if (!isAdmin && cityNames.isNotEmpty()) {
                            filter { or { cityNames.forEach { eq("cidade", it) } } }
                        }
                        order("capturado_em", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(500)
                    }.decodeList<Customer>()
                }
                
                remoteCustomers = list
                combineAndEmit()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [RECADASTRO] Falha: ${e.message}")
            }
        }
    }

    override fun updateLocalCustomers(localCustomers: List<Customer>) {
        localPendingCustomers = localCustomers.map { it.copy(isSynced = false) }
        combineAndEmit()
    }

    override fun clearCache() {
        remoteCustomers = emptyList()
        localPendingCustomers = emptyList()
        _customers.value = emptyList()
    }

    private fun combineAndEmit() {
        scope.launch(Dispatchers.Default) {
            refreshMutex.withLock {
                // SÊNIOR SMART MERGE: Identificamos quais IDs já estão no servidor
                val remoteIds = remoteCustomers.mapNotNull { it.id }.toSet()

                val combined = (localPendingCustomers.asSequence().map { 
                    // Se o item local já foi detectado no servidor, mudamos o status visual IMEDIATAMENTE
                    if (remoteIds.contains(it.id)) it.copy(isSynced = true) else it
                } + remoteCustomers.asSequence())
                .distinctBy { it.id ?: "TEMP_${System.nanoTime()}" }
                .toList()
                
                _customers.value = combined
            }
        }
    }

    override suspend fun addCustomer(customer: Customer) {
        addCustomers(listOf(customer))
    }

    override suspend fun addCustomers(customers: List<Customer>) {
        if (customers.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                client.postgrest["clientes"].upsert(customers)
                
                // SÊNIOR FIX: Recuperamos o perfil atual para garantir que o fetch ocorra com os filtros certos
                val profile = AuthRepositoryImpl.getInstance().currentUserProfile.value
                fetchCustomers(
                    cidadeId = profile?.cidadeId,
                    isAdmin = profile?.cargo?.lowercase() == "desenvolvedor"
                )
            } catch (e: Exception) {
                Log.e("SyncDebug", "❌ ERRO NO LOTE: ${e.message}")
                throw e
            }
        }
    }

    override suspend fun updateCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            client.postgrest["clientes"].insert(customer)
            fetchCustomers()
        }
    }

    override suspend fun getCustomerById(id: String): Customer? {
        return _customers.value.find { it.id == id }
    }

    override suspend fun deleteCustomer(id: String) {
        withContext(Dispatchers.IO) {
            client.postgrest["clientes"].delete { filter { eq("id", id) } }
            fetchCustomers()
        }
    }

    override suspend fun searchCustomersRemote(query: String, cidadeId: String?, isAdmin: Boolean): List<Customer> {
        return withContext(Dispatchers.IO) {
            try {
                val userCityName = com.example.oaplicativo.util.CityUtils.getFriendlyCityName(cidadeId)
                client.postgrest["clientes"].select {
                    if (!isAdmin && userCityName != null) filter { eq("cidade", userCityName) }
                    filter { or { ilike("name", "%$query%"); eq("matricula", query) } }
                    limit(50)
                }.decodeList<Customer>()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SEARCH_REMOTE] Falha: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun saveCustomerLocallyAndSync(customer: Customer) {
        withContext(Dispatchers.IO) {
            try {
                val ctx = applicationContext ?: return@withContext
                val db = com.example.oaplicativo.data.local.LocalDatabase.getInstance(ctx)
                db.saveCustomerOffline(customer)
                val pending = db.getPendingCustomers().map { it.second }
                updateLocalCustomers(pending)
                
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>()
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .build()
                
                androidx.work.WorkManager.getInstance(ctx).enqueueUniqueWork(
                    "sync_now_${System.currentTimeMillis()}",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            } catch (e: Exception) {
                Log.e("debugs", "❌ [REPO] Falha ao iniciar ciclo: ${e.message}")
            }
        }
    }

    companion object {
        @Volatile private var instance: CustomerRepositoryImpl? = null
        fun getInstance(): CustomerRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: CustomerRepositoryImpl().also { instance = it }
            }
        }
    }
}
