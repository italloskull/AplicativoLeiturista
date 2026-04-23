package com.example.oaplicativo.ui.screens.customer_form

import androidx.lifecycle.ViewModel
import com.example.oaplicativo.data.CustomerRepository
import com.example.oaplicativo.model.Customer

class CustomerFormViewModel(
    private val repository: CustomerRepository = CustomerRepository.getInstance()
) : ViewModel() {
    
    fun saveCustomer(customer: Customer) {
        if (customer.id.isEmpty()) {
            repository.addCustomer(customer)
        } else {
            repository.updateCustomer(customer)
        }
    }

    fun getCustomer(id: String?): Customer? {
        return id?.let { repository.getCustomerById(it) }
    }
}