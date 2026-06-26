package com.example.oaplicativo.ui.screens.user_registration

import android.util.Log
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
                // 1. Busca a resposta bruta para inspeção visual no Logcat
                val result = SupabaseClient.client.postgrest["cidades"]
                    .select()
                
                Log.d("PROD_DEBUG", "📡 Status: OK | Body: ${result.data}")

                // 2. Tenta a decodificação manual para capturar erro exato de schema
                val list = try {
                    result.decodeList()
                } catch (decodeError: Exception) {
                    Log.e("PROD_DEBUG", "❌ Falha no Mapeamento JSON: ${decodeError.message}")
                    emptyList<Cidade>()
                }

                Log.d("PROD_DEBUG", "✅ Finalizado: ${list.size} cidades carregadas.")
                _cidades.value = list
            } catch (e: Exception) {
                Log.e("PROD_DEBUG", "❌ Falha de Rede ou Autenticação: ${e.message}", e)
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
