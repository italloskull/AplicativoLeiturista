package com.example.oaplicativo.data

import android.util.Log
import com.example.oaplicativo.model.Customer
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CustomerRepository {
    private val client = SupabaseClient.client
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    suspend fun fetchCustomers() {
        withContext(Dispatchers.IO) {
            try {
                val list = client.postgrest["customers"]
                    .select {
                        order("created_at", order = Order.DESCENDING)
                    }.decodeList<Customer>()
                _customers.value = list
            } catch (e: Exception) {
                Log.e("CustomerRepository", "Erro ao buscar clientes", e)
            }
        }
    }

    suspend fun addCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            try {
                // Remove ID and createdAt to let Supabase generate them
                val customerToSave = customer.copy(id = null, createdAt = null)
                client.postgrest["customers"].insert(customerToSave)
                fetchCustomers()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun updateCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            try {
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
                    filter {
                        eq("id", customer.id ?: "")
                    }
                }
                fetchCustomers()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun getCustomerById(id: String): Customer? {
        return _customers.value.find { it.id == id }
    }

    suspend fun deleteCustomer(id: String) {
        withContext(Dispatchers.IO) {
            try {
                client.postgrest["customers"].delete {
                    filter {
                        eq("id", id)
                    }
                }
                fetchCustomers()
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    companion object {
        @Volatile
        private var instance: CustomerRepository? = null
        fun getInstance(): CustomerRepository {
            return instance ?: synchronized(this) {
                instance ?: CustomerRepository().also { instance = it }
            }
        }
    }
}
