package com.example.oaplicativo.ui.screens.user_registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserRegistrationViewModel(
    private val authRepository: AuthRepository = AuthRepository.getInstance()
) : ViewModel() {
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    fun register(fullName: String, username: String, email: String, pass: String, cargo: String) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                authRepository.registerUser(fullName, username, email, pass, cargo)
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Erro desconhecido")
                e.printStackTrace()
            }
        }
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}