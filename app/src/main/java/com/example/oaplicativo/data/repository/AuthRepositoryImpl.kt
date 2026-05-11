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
        val trimmedIdentifier = identifier.trim().lowercase()

        // SOLUÇÃO: Restaurada busca de e-mail via RPC por Nome de Usuário
        val email = try {
            val result = client.postgrest.rpc(
                "get_email_by_username",
                buildJsonObject { put("username_param", trimmedIdentifier) }
            )
            val raw = result.data.trim().removeSurrounding("\"")
            if (raw.isBlank() || raw == "null") throw Exception("Usuário não encontrado")
            raw
        } catch (e: Exception) {
            Log.e("AuthRepo", "Erro no RPC: ${e.message}")
            throw Exception("Usuário não encontrado")
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
                _currentUserProfile.value = profile
            } catch (e: Exception) {
                Log.e("AuthRepo", "Erro ao carregar perfil: ${e.message}")
                _currentUserProfile.value = null
            }
        }
    }

    override suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        sector: String,
        role: String,
        cidadeId: String
    ) {
        val payload = buildJsonObject {
            put("email", email.trim().lowercase())
            put("password", password.trim())
            put("full_name", name.trim())
            put("username", sector.trim())
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
        client.auth.signOut()
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
