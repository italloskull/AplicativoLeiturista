package com.example.oaplicativo.ui.screens.customer_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.AuthRepository
import com.example.oaplicativo.data.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CustomerListViewModel(
    private val customerRepository: CustomerRepository = CustomerRepository.getInstance(),
    private val authRepository: AuthRepository = AuthRepository.getInstance()
) : ViewModel() {
    val customers: StateFlow<List<Customer>> = customerRepository.customers
    val currentUserProfile: StateFlow<UserProfile?> = authRepository.currentUserProfile

    init {
        refreshCustomers()
    }

    fun refreshCustomers() {
        viewModelScope.launch {
            customerRepository.fetchCustomers()
        }
    }
}
