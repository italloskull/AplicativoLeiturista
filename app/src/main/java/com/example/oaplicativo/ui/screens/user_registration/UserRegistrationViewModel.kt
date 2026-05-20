package com.example.oaplicativo.ui.screens.user_registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.model.Cidade
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserRegistrationViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
) : ViewModel() {

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    private val _cidades = MutableStateFlow<List<Cidade>>(emptyList())
    val cidades: StateFlow<List<Cidade>> = _cidades.asStateFlow()

    fun loadCidades() {
        viewModelScope.launch {
            try {
                val list = SupabaseClient.client.postgrest["cidades"].select().decodeList<Cidade>()
                _cidades.value = list
            } catch (e: Exception) {
                _cidades.value = emptyList()
            }
        }
    }

    fun register(name: String, email: String, pass: String, user: String, role: String, cidadeId: String) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                authRepository.registerUser(name, email, pass, user, role, cidadeId)
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Erro ao registrar")
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
