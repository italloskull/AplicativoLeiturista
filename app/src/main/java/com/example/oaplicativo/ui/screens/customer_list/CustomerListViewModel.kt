@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.customer_list

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerListViewModel(
    application: Application,
    private val customerRepository: CustomerRepository = CustomerRepositoryImpl.getInstance(),
    private val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
) : AndroidViewModel(application) {
    
    val customers: StateFlow<List<Customer>> = customerRepository.customers
    val currentUserProfile: StateFlow<UserProfile?> = authRepository.currentUserProfile

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val localDb = LocalDatabase.getInstance(application)

    init {
        loadData()
        startPeriodicRefresh()
    }

    fun loadData() {
        viewModelScope.launch {
            updateLocalData()
            customerRepository.fetchCustomers()
        }
    }

    private fun updateLocalData() {
        val pending = localDb.getPendingCustomers().map { it.second }
        customerRepository.updateLocalCustomers(pending)
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // Atualiza a cada 30s se a tela estiver aberta
                updateLocalData()
                customerRepository.fetchCustomers()
            }
        }
    }

    fun refreshCustomers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                updateLocalData()
                customerRepository.fetchCustomers()
            } catch (e: Exception) {
                Log.e("CustomerListVM", "Erro ao atualizar: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
