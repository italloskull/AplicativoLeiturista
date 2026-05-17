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
import kotlinx.coroutines.launch

class UserRegistrationViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
) : ViewModel() {

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    private val _cidades = MutableStateFlow<List<Cidade>>(emptyList())
    val cidades: StateFlow<List<Cidade>> = _cidades

    fun loadCidades() {
        viewModelScope.launch {
            try {
                val lista = SupabaseClient.client.postgrest["cidades"]
                    .select()
                    .decodeList<Cidade>()
                _cidades.value = lista
            } catch (e: Exception) {
                // silencia, lista fica vazia
            }
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        pass: String,
        cargo: String,
        cidadeId: String
    ) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                authRepository.registerUser(
                    name = fullName,
                    email = email,
                    password = pass,
                    username = username,
                    role = cargo,
                    cidadeId = cidadeId
                )
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(
                    e.message ?: "Falha na comunicação com o servidor"
                )
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
