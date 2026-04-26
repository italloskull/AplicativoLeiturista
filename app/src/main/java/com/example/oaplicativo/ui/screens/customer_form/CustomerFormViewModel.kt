package com.example.oaplicativo.ui.screens.customer_form

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CustomerFormState {
    object Idle : CustomerFormState()
    object Loading : CustomerFormState()
    object Success : CustomerFormState()
    data class Error(val message: String) : CustomerFormState()
}

class CustomerFormViewModel(
    application: Application,
    private val repository: CustomerRepository = CustomerRepositoryImpl.getInstance(),
    private val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
) : AndroidViewModel(application) {
    
    private val _state = MutableStateFlow<CustomerFormState>(CustomerFormState.Idle)
    val state: StateFlow<CustomerFormState> = _state.asStateFlow()

    val currentUserProfile: StateFlow<UserProfile?> = authRepository.currentUserProfile

    private val db = LocalDatabase(application)
    private val workManager = WorkManager.getInstance(application)

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            _state.value = CustomerFormState.Loading
            try {
                // PASSO 1: Salva no banco local (Garante que não se perca)
                db.saveCustomerOffline(customer)
                
                // PASSO 2: Atualiza a UI local imediatamente
                updateRepositoryWithLocalData()

                // PASSO 3: Sincronização única via WorkManager
                scheduleSync()

                _state.value = CustomerFormState.Success
            } catch (e: Exception) {
                _state.value = CustomerFormState.Error(e.message ?: "Erro ao salvar cliente")
            }
        }
    }

    private fun updateRepositoryWithLocalData() {
        val pending = db.getPendingCustomers().map { it.second }
        repository.updateLocalCustomers(pending)
    }

    private fun scheduleSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()

        workManager.enqueue(syncRequest)
    }

    fun getCustomer(id: String?): Customer? {
        var customer: Customer? = null
        if (id != null) {
            viewModelScope.launch {
                customer = repository.getCustomerById(id)
            }
        }
        return customer
    }

    fun resetState() {
        _state.value = CustomerFormState.Idle
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            _state.value = CustomerFormState.Loading
            try {
                repository.deleteCustomer(id)
                _state.value = CustomerFormState.Success
            } catch (e: Exception) {
                _state.value = CustomerFormState.Error(e.message ?: "Erro ao excluir cliente")
            }
        }
    }
}