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
import com.example.oaplicativo.model.Cidade
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

    private val _authorizedCities = MutableStateFlow<List<Cidade>>(emptyList())
    val authorizedCities: StateFlow<List<Cidade>> = _authorizedCities.asStateFlow()

    private val _selectedCityFilter = MutableStateFlow<Cidade?>(null) // null = "Todas"
    val selectedCityFilter: StateFlow<Cidade?> = _selectedCityFilter.asStateFlow()

    private val localDb = LocalDatabase.getInstance(application)

    init {
        loadData()
        loadAuthorizedCities()
    }

    private fun loadAuthorizedCities() {
        viewModelScope.launch {
            _authorizedCities.value = authRepository.getUserCities()
        }
    }

    fun selectCityFilter(cidade: Cidade?) {
        _selectedCityFilter.value = cidade
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
                customerRepository.fetchCustomers(
                    cidadeId = profile?.cidadeId,
                    isAdmin = profile?.isDeveloper == true
                )
                loadAuthorizedCities()
            } catch (e: Exception) {
                Log.e("CustomerListVM", "Erro ao atualizar: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // SÊNIOR SMART SEARCH: Busca remota se a local falhar ou para busca exaustiva
    fun searchRemote(query: String) {
        if (query.isBlank() || query.length < 2) return
        
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val profile = authRepository.currentUserProfile.value
                val selectedCity = _selectedCityFilter.value
                
                // SÊNIOR BI FILTER: Filtra pela cidade selecionada ou pela cidade do perfil (Segurança)
                val targetCityId = selectedCity?.id ?: profile?.cidadeId
                val isAdmin = profile?.cargo?.lowercase().let { it == "administrador" || it == "desenvolvedor" }

                val remoteResults = customerRepository.searchCustomersRemote(
                    query = query,
                    cidadeId = targetCityId,
                    isAdmin = isAdmin
                )
                
                if (remoteResults.isNotEmpty()) {
                    customerRepository.addCustomers(remoteResults)
                    Log.d("debugs", "✅ [SEARCH_REMOTE] Encontrados ${remoteResults.size} registros para '$query'.")
                }
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SEARCH_REMOTE] Falha: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
