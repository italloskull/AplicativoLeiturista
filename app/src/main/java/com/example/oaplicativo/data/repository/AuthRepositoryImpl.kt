@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl private constructor() : AuthRepository {
    private val client = SupabaseClient.client
    
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    override suspend fun login(identifier: String, password: String) {
        // A lógica de login agora é delegada ao ViewModel para suportar o fallback contextual
        // Mas o Repository ainda mantém a lógica base de rede
        val trimmedIdentifier = identifier.trim().lowercase()

        val email = try {
            val result = client.postgrest.rpc(
                "get_email_by_username",
                buildJsonObject { put("username_param", trimmedIdentifier) }
            )
            val raw = result.data.trim().removeSurrounding("\"")
            if (raw.isBlank() || raw == "null") throw Exception("Usuário não encontrado")
            raw
        } catch (e: Exception) {
            val message = e.message ?: ""
            Log.e("AuthRepo", "Erro na fase de descoberta de e-mail: $message", e)
            
            // SÊNIOR FIX: Identificação robusta de erros de conectividade
            if (message.contains("Network", ignoreCase = true) || 
                message.contains("timeout", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("Failed to connect", ignoreCase = true)) {
                Log.w("AuthRepo", "Conectividade ausente detectada. Acionando modo offline.")
                throw Exception("OFFLINE_ERROR")
            } else {
                throw Exception("Usuário não encontrado")
            }
        }

        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            fetchProfile()
        } catch (e: Exception) {
            Log.e("AuthRepo", "Login failed: ${e.message}")
            throw Exception("Senha incorreta ou erro de acesso.")
        }
    }

    override suspend fun fetchProfile() {
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            try {
                val profile = client.postgrest["perfis_usuario"]
                    .select {
                        filter { eq("id", user.id) }
                    }.decodeSingleOrNull<UserProfile>()
                
                // UNIFICAÇÃO: Notifica o StateFlow
                _currentUserProfile.value = profile
            } catch (e: Exception) {
                Log.e("AuthRepo", "Erro ao carregar perfil: ${e.message}")
            }
        }
    }

    // Método para login local (Modo Sênior)
    fun setLocalProfile(profile: UserProfile?) {
        _currentUserProfile.value = profile
    }

    override suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        username: String,
        role: String,
        cidadeId: String
    ) {
        val payload = buildJsonObject {
            put("email", email.trim().lowercase())
            put("password", password.trim())
            put("full_name", name.trim())
            put("username", username.trim())
            put("cargo", role.trim())
            put("cidade_id", cidadeId)
        }

        try {
            client.functions.invoke("create-user") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Erro desconhecido"
            Log.e("AuthRepo", "Erro ao criar usuário: $msg")
            throw Exception(msg)
        }
    }

    override suspend fun logout() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {}
        _currentUserProfile.value = null
    }

    companion object {
        @Volatile
        private var instance: AuthRepositoryImpl? = null
        fun getInstance(): AuthRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: AuthRepositoryImpl().also { instance = it }
            }
        }
    }
}
