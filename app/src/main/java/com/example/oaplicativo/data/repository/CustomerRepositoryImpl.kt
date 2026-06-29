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
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val refreshMutex = Mutex()

    private var remoteCustomers: List<Customer> = emptyList()
    private var localPendingCustomers: List<Customer> = emptyList()
    
    private var realtimeChannel: RealtimeChannel? = null

    init {
        scope.launch {
            fetchCustomers()
        }
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
                    
                    myChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "clientes"
                    }.collect {
                        delay(800)
                        fetchCustomers()
                    }
                    
                    myChannel.subscribe()
                    break
                } catch (e: Exception) {
                    delay(10000)
                }
            }
        }
    }

    override suspend fun fetchCustomers(cidadeId: String?, isAdmin: Boolean) {
        refreshMutex.withLock {
            try {
                val userCityName = when(cidadeId) {
                    "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
                    "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
                    "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
                    "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
                    "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
                    else -> null
                }

                Log.d("debugs", "🔍 [RECADASTRO] Iniciando busca... Cidade: $userCityName | GodMode: $isAdmin")
                
                val list = withContext(Dispatchers.IO) {
                    client.postgrest["clientes"]
                        .select {
                            // SÊNIOR FIX: Injeção de Filtro Territorial via Nome Legível
                            if (!isAdmin && userCityName != null) {
                                filter { eq("cidade", userCityName) }
                            }
                            order("criado_em", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(150)
                        }.decodeList<Customer>()
                }
                
                Log.d("debugs", "✅ [RECADASTRO] Recebidos ${list.size} itens da nuvem.")
                
                // SÊNIOR DEBUG: Se a lista veio vazia, avisa no log para checarmos o RLS ou o ID
                if (list.isEmpty()) {
                    Log.w("debugs", "⚠️ [RECADASTRO] Supabase retornou ZERO. Verifique se os registros no banco possuem o cidade_id: $cidadeId")
                }

                remoteCustomers = list
                combineAndEmit()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val shortError = when {
                    msg.contains("Unable to resolve host") -> "Sem Internet (DNS)"
                    msg.contains("timeout") -> "Tempo Esgotado"
                    msg.contains("401") || msg.contains("403") -> "Acesso Negado"
                    else -> "Falha de Conexão"
                }
                Log.e("debugs", "❌ [RECADASTRO] Falha: $shortError | Detalhe: ${e.message}")
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
                val profile = AuthRepositoryImpl.getInstance().currentUserProfile.value
                val isDev = profile?.cargo?.lowercase() == "desenvolvedor"
                val userCidadeId = profile?.cidadeId
                val userCityName = when(userCidadeId) {
                    "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
                    "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
                    "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
                    "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
                    "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
                    else -> null
                }

                // SÊNIOR FIX: Blindagem Territorial Absoluta via Nome Legível
                // Remove qualquer dado que não pertença à cidade do usuário logado antes de mostrar na tela
                val combined = (localPendingCustomers + remoteCustomers)
                    .filter { item ->
                        isDev || userCityName == null || item.cidade == userCityName
                    }
                    .distinctBy { it.id ?: UUID.randomUUID().toString() }
                
                _customers.value = combined
                Log.d("debugs", "📊 [RECADASTRO] Lista combinada emitida: ${combined.size} clientes únicos.")
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
                // SÊNIOR PERF: Envio em Lote (Batch Insert) para economizar rádio e bateria
                Log.d("SyncDebug", "🚀 Enviando lote de ${customers.size} registros.")
                
                client.postgrest["clientes"].upsert(customers) {
                    onConflict = "id"
                }
                
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
            client.postgrest["clientes"].delete {
                filter { eq("id", id) }
            }
            fetchCustomers()
        }
    }

    override suspend fun saveCustomerLocallyAndSync(customer: Customer) { }

    companion object {
        @Volatile
        private var instance: CustomerRepositoryImpl? = null
        fun getInstance(): CustomerRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: CustomerRepositoryImpl().also { instance = it }
            }
        }
    }
}
