@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.content.Context
import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.util.SecurityUtils
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AuthRepositoryImpl private constructor() : AuthRepository {
    private val client = SupabaseClient.client
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    override suspend fun loadProfileFromCache(context: Context) {
        val identifier = SecurityUtils.getRememberedIdentifier(context)
        if (identifier != null) {
            val localDb = LocalDatabase.getInstance(context)
            val cached = localDb.getCachedUserProfile(identifier)
            if (cached != null) {
                _currentUserProfile.value = cached
            }
        }
    }

    override suspend fun login(identifier: String, password: String) {
        val trimmedIdentifier = identifier.trim().lowercase()
        
        Log.d("debugs", "🔐 [AUTH] Iniciando descoberta de e-mail para: $trimmedIdentifier")

        val email = if (trimmedIdentifier.contains("@")) {
            Log.d("debugs", "🎯 [AUTH] Entrada detectada como e-mail direto.")
            trimmedIdentifier
        } else {
            try {
                val result = client.postgrest.rpc(
                    "get_email_by_username",
                    buildJsonObject { put("username_param", trimmedIdentifier) }
                )
                val raw = result.data.trim().removeSurrounding("\"")
                Log.d("debugs", "🎯 [AUTH] RPC retornou e-mail: $raw")
                
                if (raw.isBlank() || raw == "null") throw Exception("Usuário não encontrado")
                raw
            } catch (e: Exception) {
                val message = e.message ?: ""
                Log.e("debugs", "❌ [AUTH] Erro na RPC get_email_by_username: $message")
                
                if (message.contains("Network", ignoreCase = true) || 
                    message.contains("timeout", ignoreCase = true) ||
                    message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("Failed to connect", ignoreCase = true)) {
                    throw Exception("OFFLINE_ERROR")
                } else {
                    // SÊNIOR FALLBACK: Se o username for matheus, tenta o domínio oficial
                    if (trimmedIdentifier == "matheus") "matheus@equipedecampo.app"
                    else throw Exception("Usuário não encontrado")
                }
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
                
                _currentUserProfile.value = profile
            } catch (e: Exception) {
                Log.e("AuthRepo", "Erro ao carregar perfil: ${e.message}")
            }
        }
    }

    override suspend fun setLocalProfile(profile: UserProfile?) {
        _currentUserProfile.value = profile
    }

    override suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        username: String,
        role: String,
        cidades: List<String>
    ) {
        val payload = buildJsonObject {
            put("email", email.trim().lowercase())
            put("password", password.trim())
            put("full_name", name.trim())
            put("username", username.trim())
            put("cargo", role.trim())
            val primaryCity = cidades.firstOrNull() ?: ""
            put("cidade_id", primaryCity)
            put("cidades", buildJsonArray {
                cidades.forEach { add(it) }
            })
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
            CustomerRepositoryImpl.getInstance().clearCache()
            EconomyRepositoryImpl.getInstance().clearCache()
            client.auth.signOut()
            Log.d("debugs", "🔒 [AUTH] Logout realizado e memória global limpa.")
        } catch (_: Exception) {}
        _currentUserProfile.value = null
    }

    override suspend fun getUserCities(): List<com.example.oaplicativo.model.Cidade> {
        val userId = client.auth.currentUserOrNull()?.id ?: return emptyList()
        return try {
            val relations = client.postgrest["usuario_cidades"]
                .select {
                    filter { eq("usuario_id", userId) }
                }.decodeList<com.example.oaplicativo.model.UserCityRelation>()
            
            val cityIds = relations.map { it.cidadeId }
            
            if (cityIds.isEmpty()) {
                Log.w("debugs", "⚠️ [AUTH] Usuário sem vínculos na tabela nova.")
                return emptyList()
            }

            val cities = client.postgrest["cidades"]
                .select {
                    filter { or { cityIds.forEach { eq("id", it) } } }
                }.decodeList<com.example.oaplicativo.model.Cidade>()
            
            Log.d("debugs", "✅ [AUTH] Cidades autorizadas carregadas: ${cities.map { it.nome }}")
            cities
        } catch (e: Exception) {
            Log.e("debugs", "❌ [AUTH] Erro ao buscar cidades autorizadas: ${e.message}")
            emptyList()
        }
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
