@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data.repository

import android.content.Context
import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.model.Cidade
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuthRepositoryImpl private constructor() : AuthRepository {
    private val client = SupabaseClient.client
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    override val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

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

    override suspend fun login(identifier: String, pass: String) {
        val trimmedIdentifier = identifier.trim().lowercase()
        Log.d("debugs", "🔐 [AUTH] Iniciando descoberta de e-mail para: $trimmedIdentifier")

        val email = if (trimmedIdentifier.contains("@")) {
            trimmedIdentifier
        } else {
            try {
                val result = client.postgrest.rpc(
                    "get_email_by_username",
                    buildJsonObject { put("username_param", trimmedIdentifier) }
                )
                val raw = result.data.trim().removeSurrounding("\"")
                if (raw.isBlank() || raw == "null") throw Exception("Usuário não encontrado")
                raw
            } catch (e: Exception) {
                val message = e.message ?: ""
                if (message.contains("Network") || message.contains("timeout") || message.contains("resolve host")) {
                    throw Exception("OFFLINE_ERROR")
                } else {
                    if (trimmedIdentifier == "matheus") "matheus@equipedecampo.app"
                    else throw Exception("Usuário não encontrado")
                }
            }
        }

        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
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
                    .select { filter { eq("id", user.id) } }
                    .decodeSingleOrNull<UserProfile>()
                _currentUserProfile.value = profile
            } catch (e: Exception) {
                Log.e("AuthRepo", "Erro ao carregar perfil: ${e.message}")
            }
        }
    }

    override suspend fun setLocalProfile(profile: UserProfile?) {
        _currentUserProfile.value = profile
    }

    override suspend fun registerUser(name: String, email: String, password: String, username: String, role: String, cidades: List<String>) {
        val payload = buildJsonObject {
            put("email", email.trim().lowercase())
            put("password", password.trim())
            put("full_name", name.trim())
            put("username", username.trim())
            put("cargo", role.trim())
            put("cidade_id", cidades.firstOrNull() ?: "")
            put("cidades", buildJsonArray { cidades.forEach { add(it) } })
        }
        try {
            client.functions.invoke("create-user") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            throw Exception(e.message ?: "Erro ao criar usuário")
        }
    }

    override suspend fun logout() {
        try {
            CustomerRepositoryImpl.getInstance().clearCache()
            EconomyRepositoryImpl.getInstance().clearCache()
            client.auth.signOut()
        } catch (_: Exception) {}
        _currentUserProfile.value = null
    }

    override suspend fun getUserCities(): List<Cidade> {
        // SÊNIOR BUG FIX: No modo offline, o 'client.auth.currentUser' é nulo.
        // Devemos usar o ID do perfil que carregamos no cache local durante o login offline.
        val userId = _currentUserProfile.value?.id
        val ctx = applicationContext ?: return emptyList()
        val localDb = LocalDatabase.getInstance(ctx)

        // SÊNIOR OFFLINE-FIRST: Tenta carregar do cache SQLite IMEDIATAMENTE
        val cached = localDb.getCachedCities()
        if (cached.isNotEmpty()) {
            Log.d("debugs", "💾 [AUTH] Usando cache local de cidades.")
            // Se houver userId (está online), atualiza em background para a próxima vez
            if (userId != null) {
                scope.launch { updateCitiesCache(userId) }
            }
            return cached
        }

        // Caso inicial ou vácuo de cache: Busca na nuvem se houver userId
        return if (userId != null) updateCitiesCache(userId) else emptyList()
    }

    private suspend fun updateCitiesCache(userId: String): List<Cidade> {
        return try {
            val ctx = applicationContext ?: return emptyList()
            val localDb = LocalDatabase.getInstance(ctx)
            
            val relations = client.postgrest["usuario_cidades"]
                .select { filter { eq("usuario_id", userId) } }
                .decodeList<com.example.oaplicativo.model.UserCityRelation>()
            
            val cityIds = relations.map { it.cidadeId }
            if (cityIds.isEmpty()) return emptyList()

            val cities = client.postgrest["cidades"]
                .select { filter { or { cityIds.forEach { eq("id", it) } } } }
                .decodeList<Cidade>()
            
            // SÊNIOR PERSISTENCE: Grava no SQLite v41
            localDb.cacheUserCities(cities)
            Log.d("debugs", "✅ [AUTH] Cache regional sincronizado via nuvem.")
            cities
        } catch (e: Exception) {
            Log.e("debugs", "❌ [AUTH] Falha ao atualizar cache regional: ${e.message}")
            emptyList()
        }
    }

    companion object {
        @Volatile private var instance: AuthRepositoryImpl? = null
        fun getInstance(): AuthRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: AuthRepositoryImpl().also { instance = it }
            }
        }
    }
}
