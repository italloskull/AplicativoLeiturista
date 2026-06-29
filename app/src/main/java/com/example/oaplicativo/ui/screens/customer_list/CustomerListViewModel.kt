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
    }

    fun loadData() {
        viewModelScope.launch {
            updateLocalData()
            val profile = authRepository.currentUserProfile.value
            customerRepository.fetchCustomers(
                cidadeId = profile?.cidadeId,
                isAdmin = profile?.isDeveloper == true
            )
        }
    }

    private fun updateLocalData() {
        val pending = localDb.getPendingCustomers().map { it.second }
        customerRepository.updateLocalCustomers(pending)
    }


    fun refreshCustomers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                updateLocalData()
                val profile = authRepository.currentUserProfile.value
                // SÊNIOR FIX: Repassa o filtro de cidade para o repositório
                // DESENVOLVEDOR ignora o filtro (God Mode)
                customerRepository.fetchCustomers(
                    cidadeId = profile?.cidadeId,
                    isAdmin = profile?.isDeveloper == true
                )
            } catch (e: Exception) {
                Log.e("CustomerListVM", "Erro ao atualizar: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
