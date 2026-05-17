@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.util.Log
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

    override suspend fun fetchCustomers() {
        refreshMutex.withLock {
            try {
                val list = withContext(Dispatchers.IO) {
                    client.postgrest["clientes"]
                        .select {
                            order("criado_em", order = Order.DESCENDING)
                            limit(100)
                        }.decodeList<Customer>()
                }
                remoteCustomers = list
                combineAndEmit()
            } catch (e: Exception) {
                Log.e("CustomerRepositoryImpl", "Erro ao buscar dados: ${e.message}")
            }
        }
    }

    override fun updateLocalCustomers(localCustomers: List<Customer>) {
        localPendingCustomers = localCustomers.map { it.copy(isSynced = false) }
        combineAndEmit()
    }

    private fun combineAndEmit() {
        scope.launch(Dispatchers.Default) { // OTIMIZAÇÃO: Processamento em pool paralelo (não Main Thread)
            refreshMutex.withLock {
                val remoteKeys = remoteCustomers.mapNotNull { it.registrationNumber }.filter { it.isNotBlank() }.toSet()
                val uniqueLocal = localPendingCustomers.filter { local ->
                    val key = local.registrationNumber
                    // SÊNIOR FIX: Se a matrícula for vazia, nunca filtra. Se tiver, checa duplicidade.
                    if (key.isNullOrBlank()) true 
                    else !remoteKeys.contains(key)
                }
                val combined = uniqueLocal + remoteCustomers
                _customers.value = combined
            }
        }
    }

    override suspend fun addCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("CustomerRepo", "Enviando para Supabase: ${customer.registrationNumber}")
                // Usa ID como critério de conflito para permitir edições locais sincronizarem sem duplicar
                client.postgrest["clientes"].upsert(customer) {
                    onConflict = "id"
                }
                fetchCustomers()
            } catch (e: Exception) {
                Log.e("CustomerRepo", "FALHA NO SUPABASE: ${e.message}", e)
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

    override suspend fun saveCustomerLocallyAndSync(customer: Customer) {
        // Método preparado para centralização futura de salvamento + sync
        // Mantido vázio para garantir zero quebras de build nesta fase de reorganização
    }

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
