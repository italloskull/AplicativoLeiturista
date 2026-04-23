package com.example.oaplicativo.data

import com.example.oaplicativo.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CustomerRepository {
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    fun addCustomer(customer: Customer) {
        _customers.update { list ->
            list + customer.copy(id = (list.size + 1).toString())
        }
    }

    fun updateCustomer(updatedCustomer: Customer) {
        _customers.update { list ->
            list.map { if (it.id == updatedCustomer.id) updatedCustomer else it }
        }
    }

    fun getCustomerById(id: String): Customer? {
        return _customers.value.find { it.id == id }
    }
    
    companion object {
        // Simple Singleton for demonstration
        private var instance: CustomerRepository? = null
        fun getInstance(): CustomerRepository {
            if (instance == null) {
                instance = CustomerRepository()
            }
            return instance!!
        }
    }
}