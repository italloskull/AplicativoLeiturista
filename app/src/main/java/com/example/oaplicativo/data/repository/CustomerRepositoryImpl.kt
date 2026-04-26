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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
            while (true) { // Loop de reconexão automática resiliente (SRE)
                try {
                    // Limpa canal anterior se existir para evitar vazamento de memória
                    realtimeChannel?.unsubscribe()
                    
                    client.realtime.connect()
                    val myChannel = client.realtime.channel("public_customers")
                    realtimeChannel = myChannel
                    
                    myChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "customers"
                    }.collect {
                        Log.d("CustomerRepositoryImpl", "Realtime: Mudança detectada no servidor.")
                        delay(800)
                        fetchCustomers()
                    }
                    
                    myChannel.subscribe()
                    Log.d("CustomerRepositoryImpl", "Realtime: Conectado e Subscrito.")
                    break // Sai do loop se conectar com sucesso
                } catch (e: Exception) {
                    Log.e("CustomerRepositoryImpl", "Realtime: Falha na conexão. Tentando em 10s... Erro: ${e.message}")
                    delay(10000)
                }
            }
        }
    }

    override suspend fun fetchCustomers() {
        refreshMutex.withLock {
            try {
                val list = withContext(Dispatchers.IO) {
                    client.postgrest["customers"]
                        .select {
                            order("created_at", order = Order.DESCENDING)
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
        val remoteKeys = remoteCustomers.mapNotNull { it.registrationNumber }.toSet()
        
        val uniqueLocal = localPendingCustomers.filter { local ->
            val key = local.registrationNumber
            key != null && !remoteKeys.contains(key)
        }

        val combined = uniqueLocal + remoteCustomers
        _customers.value = combined
    }

    override suspend fun addCustomer(customer: Customer) {
        try {
            client.postgrest["customers"].insert(customer)
            Log.d("CustomerRepositoryImpl", "Sucesso no upload.")
            fetchCustomers()
        } catch (e: Exception) {
            Log.e("CustomerRepositoryImpl", "Falha no upload: ${e.message}")
            throw e
        }
    }

    override suspend fun updateCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            client.postgrest["customers"].update(
                buildJsonObject {
                    put("name", customer.name)
                    put("registration_number", customer.registrationNumber)
                    put("registration_digit", customer.registrationDigit)
                    put("email", customer.email)
                    put("landline", customer.landline)
                    put("cell_phone", customer.cellPhone)
                    put("is_standard_measurement_box", customer.isStandardMeasurementBox)
                    put("is_standardized_seals", customer.isStandardizedSeals)
                    put("is_hd_accessible", customer.isHdAccessible)
                    put("is_vacationer", customer.isVacationer)
                    put("latitude", customer.latitude)
                    put("longitude", customer.longitude)
                }
            ) {
                filter { eq("id", customer.id ?: "") }
            }
            fetchCustomers()
        }
    }

    override suspend fun getCustomerById(id: String): Customer? {
        return _customers.value.find { it.id == id }
    }

    override suspend fun deleteCustomer(id: String) {
        withContext(Dispatchers.IO) {
            client.postgrest["customers"].delete {
                filter { eq("id", id) }
            }
            fetchCustomers()
        }
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