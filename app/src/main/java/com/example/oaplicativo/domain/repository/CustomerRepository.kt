package com.example.oaplicativo.domain.repository

import com.example.oaplicativo.model.Customer
import kotlinx.coroutines.flow.StateFlow

interface CustomerRepository {
    val customers: StateFlow<List<Customer>>
    suspend fun fetchCustomers()
    suspend fun addCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun getCustomerById(id: String): Customer?
    suspend fun deleteCustomer(id: String)
    fun updateLocalCustomers(localCustomers: List<Customer>)
    suspend fun saveCustomerLocallyAndSync(customer: Customer)
}
