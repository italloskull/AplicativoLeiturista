package com.example.oaplicativo.ui.screens.customer_form

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val state: StateFlow<CustomerFormState> = _state

    val currentUserProfile: StateFlow<UserProfile?> = authRepository.currentUserProfile

    private val db = LocalDatabase(application)
    private val workManager = WorkManager.getInstance(application)

    fun saveCustomer(customer: Customer) {
        _state.value = CustomerFormState.Loading
        viewModelScope.launch {
            try {
                // Sincroniza qualidade e auditoria
                val finalCustomer = customer.copy(
                    quality = calculateQuality(customer),
                    isSynced = false
                )
                
                db.saveCustomerOffline(finalCustomer)
                updateRepositoryWithLocalData()
                scheduleSync()
                _state.value = CustomerFormState.Success
            } catch (e: Exception) {
                Log.e("CustomerFormVM", "Erro ao salvar", e)
                _state.value = CustomerFormState.Error(e.message ?: "Erro ao salvar localmente")
            }
        }
    }

    private fun calculateQuality(c: Customer): String {
        val fields = listOf(c.name, c.registrationNumber, c.email, c.cellPhone, c.locationStatus)
        val filled = fields.count { !it.isNullOrBlank() }
        val pct = (filled.toFloat() / fields.size) * 100
        return when {
            pct >= 80 -> "Boa"
            pct >= 50 -> "Regular"
            else -> "Ruim"
        }
    }

    fun updateRepositoryWithLocalData() {
        val pending = db.getPendingCustomers().map { it.second }
        repository.updateLocalCustomers(pending)
    }

    fun scheduleSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()
        workManager.enqueue(syncRequest)
    }

    fun getCustomer(id: String?): Customer? {
        if (id == null) return null
        return repository.customers.value.find { it.id == id }
    }

    fun resetState() {
        _state.value = CustomerFormState.Idle
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(id)
                _state.value = CustomerFormState.Success
            } catch (e: Exception) {
                _state.value = CustomerFormState.Error(e.message ?: "Erro ao excluir")
            }
        }
    }
}
