package com.example.oaplicativo.data

import com.example.oaplicativo.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {
    private val client = SupabaseClient.client
    
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    suspend fun login(identifier: String, pass: String) {
        val trimmedIdentifier = identifier.trim().lowercase()
        val email = if (trimmedIdentifier.contains("@")) {
            trimmedIdentifier
        } else {
            // Resolve username to email using RPC function
            val result = client.postgrest.rpc(
                "get_email_by_username",
                buildJsonObject { put("username_param", trimmedIdentifier) }
            )
            result.decodeAsOrNull<String>() ?: throw Exception("Usuário não encontrado")
        }

        client.auth.signInWith(Email) {
            this.email = email
            password = pass
        }
        fetchProfile()
    }

    private suspend fun fetchProfile() {
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

    suspend fun registerUser(fullName: String, username: String, email: String, pass: String, cargo: String) {
        client.functions.invoke("create-user") {
            setBody(buildJsonObject {
                put("email", email)
                put("password", pass)
                put("full_name", fullName)
                put("username", username)
                put("cargo", cargo)
            })
        }
    }

    suspend fun logout() {
        client.auth.signOut()
        _currentUserProfile.value = null
    }

    companion object {
        @Volatile
        private var instance: AuthRepository? = null
        fun getInstance(): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository().also { instance = it }
            }
        }
    }
}