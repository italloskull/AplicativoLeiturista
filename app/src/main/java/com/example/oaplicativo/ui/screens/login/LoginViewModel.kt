package com.example.oaplicativo.ui.screens.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl.getInstance()
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(context: Context, identifier: String, pass: String, remember: Boolean) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                authRepository.login(identifier, pass)
                
                val profile = authRepository.currentUserProfile.value
                if (profile != null) {
                    val localDb = LocalDatabase(context)
                    localDb.cacheUserProfile(
                        id = profile.id,
                        username = profile.username ?: "",
                        fullName = profile.fullName ?: "",
                        cidadeId = profile.cidadeId ?: "",
                        isAdmin = profile.isAdmin,
                        email = profile.email
                    )
                }

                if (remember) {
                    SecurityUtils.saveCredentials(context, identifier, pass, true)
                }
                _loginState.value = LoginState.Success
                
            } catch (e: Exception) {
                if (e.message == "OFFLINE_ERROR") {
                    Log.d("LoginOffline", "Iniciando processo de verificação local...")
                    val savedPass = SecurityUtils.getRememberedPassword(context)
                    val savedUser = SecurityUtils.getRememberedIdentifier(context)
                    
                    val inputUser = identifier.lowercase().trim()
                    val storedUser = savedUser?.lowercase()?.trim()

                    Log.d("LoginOffline", "Input: '$inputUser' | Stored: '$storedUser'")
                    Log.d("LoginOffline", "Senha Input: '${pass.take(1)}***' | Stored: '${savedPass?.take(1)}***'")

                    if (storedUser != null && inputUser == storedUser && pass == savedPass) {
                        val localDb = LocalDatabase(context)
                        val cachedProfile = localDb.getCachedUserProfile(inputUser)
                        
                        if (cachedProfile != null) {
                            Log.d("LoginOffline", "SUCESSO: Perfil de '$inputUser' recuperado do SQLite.")
                            (authRepository as AuthRepositoryImpl).setLocalProfile(cachedProfile)
                            _loginState.value = LoginState.Success
                        } else {
                            Log.e("LoginOffline", "FALHA: Credenciais batem, mas perfil de '$inputUser' não está no SQLite.")
                            _loginState.value = LoginState.Error("Perfil offline não encontrado para este usuário.")
                        }
                    } else {
                        Log.e("LoginOffline", "FALHA: Credenciais não conferem com o cofre criptografado.")
                        _loginState.value = LoginState.Error("Usuário ou senha incorretos (Modo Offline)")
                    }
                } else {
                    Log.e("LoginState", "Erro de autenticação normal: ${e.message}")
                    _loginState.value = LoginState.Error(e.message ?: "Erro ao autenticar")
                }
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
