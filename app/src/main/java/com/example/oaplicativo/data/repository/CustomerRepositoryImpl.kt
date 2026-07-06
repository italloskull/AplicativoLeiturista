@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.util.Log
import java.util.UUID
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
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
                    break
                } catch (_: Exception) { delay(10000) }
            }
        }
    }

    override suspend fun fetchCustomers(cidadeId: String?, isAdmin: Boolean) {
        refreshMutex.withLock {
            try {
                val userCityName = com.example.oaplicativo.util.CityUtils.getFriendlyCityName(cidadeId)
                Log.d("debugs", "🔍 [RECADASTRO] Iniciando busca... Cidade: $userCityName | GodMode: $isAdmin")
                
                val list = withContext(Dispatchers.IO) {
                    val authRepo = AuthRepositoryImpl.getInstance()
                    val authorizedCities = authRepo.getUserCities()
                    val cityNames = authorizedCities.map { it.nome }
                    
                    Log.d("debugs", "🎯 [QUERY] Cidades autorizadas no chaveiro: $cityNames")

                    client.postgrest["clientes"].select {
                        // SÊNIOR BI FIX: Busca registros de TODAS as cidades autorizadas
                        if (!isAdmin && cityNames.isNotEmpty()) {
                            filter { or { cityNames.forEach { eq("cidade", it) } } }
                        } else if (isAdmin) {
                            Log.d("debugs", "👑 [GOD_VIEW] Desenvolvedor ignorando filtros.")
                        }
                        order("capturado_em", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(500)
                    }.decodeList<Customer>()
                }
                
                Log.d("debugs", "✅ [RECADASTRO] Sucesso! ${list.size} registros baixados.")
                
                if (list.isNotEmpty()) {
                    list.take(15).forEach { 
                        Log.d("debugs", "   - Audit: ${it.name} | Cidade no Banco: '${it.cidade}' | ID: ${it.cidadeId}") 
                    }
                }
                
                if (list.isNotEmpty()) {
                    list.take(3).forEach { Log.d("debugs", "   - Registro Cloud: ${it.name} | Cidade: ${it.cidade} | Matrícula: ${it.registrationNumber}") }
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
        Log.d("debugs", "🏠 [RECADASTRO] Cache local atualizado: ${localPendingCustomers.size} pendentes.")
        combineAndEmit()
    }

    override fun clearCache() {
        remoteCustomers = emptyList()
        localPendingCustomers = emptyList()
        _customers.value = emptyList()
        Log.d("debugs", "🧹 [RECADASTRO] Cache de memória limpo.")
    }

    private fun combineAndEmit() {
        scope.launch(Dispatchers.Default) {
            refreshMutex.withLock {
                // SÊNIOR FIX: Removemos o filtro de visibilidade rigoroso aqui.
                // O filtro regional agora é feito na UI pelo seletor, permitindo que o 
                // Admin veja registros de qualquer cidade autorizada sem que eles sumam da lista.
                val combined = (localPendingCustomers + remoteCustomers)
                    .distinctBy { it.id ?: UUID.randomUUID().toString() }
                
                _customers.value = combined
                Log.d("debugs", "📊 [RECADASTRO] Lista emitida com ${combined.size} itens globais.")
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
                Log.d("SyncDebug", "🚀 Enviando lote de ${customers.size} registros.")
                client.postgrest["clientes"].upsert(customers)
                Log.d("SyncDebug", "✅ SUCESSO: Lote sincronizado.")
                fetchCustomers()
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
                Log.d("debugs", "🚀 [REPO] Sincronismo imediato acionado para: ${customer.name}")
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
