@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.util.Log
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

    override suspend fun fetchCustomers(cidadeId: String?, isAdmin: Boolean, forceFull: Boolean) {
        refreshMutex.withLock {
            try {
                val userCityName = com.example.oaplicativo.util.CityUtils.getFriendlyCityName(cidadeId)
                val ctx = applicationContext ?: return@withLock
                val db = com.example.oaplicativo.data.local.LocalDatabase.getInstance(ctx)

                // 1. SÊNIOR CACHE LOAD: Carrega o que já temos localmente para UI instantânea
                if (!forceFull) {
                    val cached = try { db.getRemoteCache(userCityName) } catch (e: Exception) { emptyList() }
                    if (cached.isNotEmpty()) {
                        remoteCustomers = cached
                        combineAndEmit()
                    }
                } else {
                    // Se for Full Refresh, limpa a lista da memória e TAMBÉM o SQLite antes de buscar
                    remoteCustomers = emptyList()
                    try { db.clearRemoteCache(userCityName) } catch (_: Exception) {}
                    combineAndEmit()
                    Log.d("debugs", "🚀 [FORCE_SYNC] Cache local limpo e iniciando carga total do servidor...")
                }

                // 2. SÊNIOR DELTA SYNC: Descobrimos o que já temos (apenas se não for carga total)
                val lastTimestamp = if (!forceFull) {
                    try { db.getLatestCacheTimestamp(userCityName) } catch (e: Exception) { null }
                } else null
                
                val list = withContext(Dispatchers.IO) {
                    val authRepo = AuthRepositoryImpl.getInstance()
                    val authorizedCities = authRepo.getUserCities()
                    val cityNames = if (userCityName != null) listOf(userCityName) else authorizedCities.map { it.nome }

                    client.postgrest["clientes"].select {
                        if (!isAdmin) {
                            val targetCities = cityNames.ifEmpty { listOf("BLOCK_ALL_ACCESS") }
                            filter { or { targetCities.forEach { ilike("cidade", it) } } }
                        }
                        
                        // SÊNIOR DELTA FILTER: Se temos um timestamp, pedimos apenas as novidades (criados ou editados)
                        if (lastTimestamp != null && lastTimestamp.isNotBlank()) {
                            filter { gt("updated_at", lastTimestamp) }
                            Log.d("debugs", "🔄 [DELTA_SYNC] Buscando novidades desde: $lastTimestamp")
                        } else {
                            // Baseline: Se não tem nada ou é Full Refresh, carrega os 1000 mais recentes
                            order("updated_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(1000)
                        }
                    }.decodeList<Customer>()
                }
                
                if (list.isNotEmpty()) {
                    // SÊNIOR MERGE LOGIC: Atualiza o que já existe e adiciona o que é novo
                    val customerMap = remoteCustomers.associateBy { it.id }.toMutableMap()
                    
                    list.forEach { updatedCustomer ->
                        val id = updatedCustomer.id ?: return@forEach
                        customerMap[id] = updatedCustomer
                    }
                    
                    remoteCustomers = customerMap.values.toList()
                    db.saveRemoteCustomersToCache(list) 
                    combineAndEmit()
                    Log.d("debugs", "✅ [SYNC] Lista atualizada com ${list.size} registros.")
                } else {
                    Log.d("debugs", "☁️ [SYNC] Nenhuma novidade encontrada.")
                }
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SYNC] Falha: ${e.message}")
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
                // SÊNIOR PERF: Indexamos os itens locais e remotos em um Map para merge instantâneo (O(N))
                // Isso substitui a busca linear dentro do loop, que era O(N²)
                val customerMap = mutableMapOf<String, Customer>()

                // 1. Processa itens remotos
                remoteCustomers.forEach { it.id?.let { id -> customerMap[id] = it } }

                // 2. Mescla com itens locais pendentes (Local sobrepõe remoto em caso de conflito de ID)
                localPendingCustomers.forEach { local ->
                    local.id?.let { id ->
                        val remoteVersion = customerMap[id]
                        customerMap[id] = if (remoteVersion != null) local.copy(isSynced = true) else local
                    }
                }

                _customers.value = customerMap.values.toList()
            }
        }
    }

    override suspend fun addCustomer(customer: Customer) {
        addCustomers(listOf(customer))
    }

    override suspend fun addCustomers(customers: List<Customer>) {
        if (customers.isEmpty()) {
            Log.d("Supauploads", "ℹ️ addCustomers chamado com lista vazia.")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                // SÊNIOR DIAGNOSTIC: Log do objeto que está sendo enviado para detectar campos nulos/inválidos
                customers.forEach { 
                    Log.d("Supauploads", "⬆️ [REPO] Tentando UPSERT Supabase: ${it.name} | ID: ${it.id} | Data: ${it.updatedAt}")
                    if (it.updatedAt.isNullOrBlank()) Log.e("Supauploads", "‼️ ALERTA: updated_at está VAZIO para ${it.name}. Isso pode causar problemas de ordenação!")
                    if (it.cidadeId.isNullOrBlank()) Log.e("Supauploads", "‼️ CRÍTICO: cidade_id está NULO para ${it.name}")
                }
                
                val response = client.postgrest["clientes"].upsert(customers) {
                    select() // SÊNIOR DIAGNOSTIC: Pede o retorno dos dados para confirmar gravação
                }
                Log.d("Supauploads", "✅ [REPO] Resposta Supabase: ${response.data}")
                
                // SÊNIOR FIX: Recuperamos o perfil atual para garantir que o fetch ocorra com os filtros certos
                val profile = AuthRepositoryImpl.getInstance().currentUserProfile.value
                fetchCustomers(
                    cidadeId = profile?.cidadeId,
                    isAdmin = profile?.cargo?.lowercase() == "desenvolvedor"
                )
            } catch (e: Exception) {
                // SÊNIOR LOGGING: Captura o erro detalhado do Supabase (Constraints, RLS, etc)
                Log.e("Supauploads", "❌ [REPO] FALHA CRÍTICA NO UPSERT SUPABASE: ${e.message}")
                Log.e("Supauploads", "❌ [REPO] Causa: ${e.cause}")
                e.printStackTrace()
                throw e
            }
        }
    }

    override suspend fun updateCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            try {
                // SÊNIOR FIX: Usamos upsert para garantir que o registro seja atualizado sem duplicar
                client.postgrest["clientes"].upsert(customer)
                
                val profile = AuthRepositoryImpl.getInstance().currentUserProfile.value
                fetchCustomers(
                    cidadeId = profile?.cidadeId,
                    isAdmin = profile?.cargo?.lowercase() == "desenvolvedor"
                )
            } catch (e: Exception) {
                Log.e("debugs", "❌ [REPO] Falha ao atualizar cliente: ${e.message}")
            }
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
                    // SÊNIOR FIX: Usamos ILIKE na cidade para ignorar case/acentos do banco
                    if (userCityName != null) filter { ilike("cidade", userCityName) }
                    
                    // SÊNIOR SEARCH FIX: Busca por Nome, Matrícula ou Hidrômetro com ILIKE (Case Insensitive)
                    filter { 
                        or { 
                            ilike("name", "%$query%")
                            ilike("matricula", "%$query%") 
                            ilike("numero_hidrometro", "%$query%")
                        } 
                    }
                    // REMOVIDO LIMIT: A busca remota agora é exaustiva em todos os registros da cidade
                }.decodeList<Customer>()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SEARCH_REMOTE] Falha: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun addRemoteResults(results: List<Customer>) {
        if (results.isEmpty()) return
        scope.launch {
            refreshMutex.withLock {
                // SÊNIOR SMART MERGE: Adiciona resultados sem duplicar e sem upsert no servidor
                val currentIds = remoteCustomers.mapNotNull { it.id }.toSet()
                val newItems = results.filter { it.id !in currentIds }
                if (newItems.isNotEmpty()) {
                    remoteCustomers = remoteCustomers + newItems
                    combineAndEmit()
                    
                    // Persiste no cache também
                    applicationContext?.let { ctx ->
                        val db = com.example.oaplicativo.data.local.LocalDatabase.getInstance(ctx)
                        db.saveRemoteCustomersToCache(newItems)
                    }
                }
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
