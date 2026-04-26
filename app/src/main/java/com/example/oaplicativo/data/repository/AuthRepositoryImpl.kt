package com.example.oaplicativo.data.repository

import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.domain.repository.AuthRepository
import com.example.oaplicativo.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
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
        val email = if (trimmedIdentifier.contains("@")) {
            trimmedIdentifier
        } else {
            val result = client.postgrest.rpc(
                "get_email_by_username",
                buildJsonObject { put("username_param", trimmedIdentifier) }
            )
            result.decodeAsOrNull<String>() ?: throw Exception("Usuário não encontrado")
        }

        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        fetchProfile()
    }

    override suspend fun fetchProfile() {
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            val profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", user.id)
                    }
                }.decodeSingleOrNull<UserProfile>()
            _currentUserProfile.value = profile
        }
    }

    override suspend fun registerUser(name: String, email: String, password: String, sector: String, role: String) {
        client.functions.invoke("create-user") {
            setBody(buildJsonObject {
                put("email", email)
                put("password", password)
                put("full_name", name)
                put("username", sector) // Mapping sector to username as per previous impl
                put("cargo", role)
            })
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