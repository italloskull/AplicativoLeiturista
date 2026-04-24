package com.example.oaplicativo.ui.screens.customer_form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.CustomerRepository
import com.example.oaplicativo.model.Customer
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
    private val repository: CustomerRepository = CustomerRepository.getInstance()
) : ViewModel() {
    
    private val _state = MutableStateFlow<CustomerFormState>(CustomerFormState.Idle)
    val state: StateFlow<CustomerFormState> = _state.asStateFlow()

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            _state.value = CustomerFormState.Loading
            try {
                if (customer.id.isNullOrEmpty()) {
                    repository.addCustomer(customer)
                } else {
                    repository.updateCustomer(customer)
                }
                _state.value = CustomerFormState.Success
            } catch (e: Exception) {
                _state.value = CustomerFormState.Error(e.message ?: "Erro ao salvar cliente")
            }
        }
    }

    fun getCustomer(id: String?): Customer? {
        return id?.let { repository.getCustomerById(it) }
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
