package com.example.oaplicativo.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.util.SecurityUtils
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(context: Context, identifier: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                authRepository.login(identifier, pass)
                SecurityUtils.saveCredentials(context, identifier, pass, rememberMe)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}