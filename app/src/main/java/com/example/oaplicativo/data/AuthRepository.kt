package com.example.oaplicativo.data

import com.example.oaplicativo.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {
    private val client = SupabaseClient.client
    
    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    suspend fun login(identifier: String, pass: String) {
        val email = if (identifier.contains("@")) {
            identifier
        } else {
            // Resolve username to email from profiles table
            val profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("username", identifier)
                    }
                }.decodeSingleOrNull<UserProfile>()
            
            profile?.email ?: throw Exception("Usuário não encontrado")
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
            try {
                val profile = client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", user.id)
                        }
                    }.decodeSingleOrNull<UserProfile>()
                _currentUserProfile.value = profile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun registerUser(fullName: String, username: String, email: String, pass: String, cargo: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            password = pass
            data = buildJsonObject {
                put("full_name", fullName)
                put("username", username)
            }
        }
        
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            // Update cargo as it's not handled by the default trigger in meta_data
            client.postgrest["profiles"].update(
                buildJsonObject {
                    put("cargo", cargo)
                }
            ) {
                filter {
                    eq("id", user.id)
                }
            }
        }
    }

    fun logout() {
        _currentUserProfile.value = null
    }

    companion object {
        private var instance: AuthRepository? = null
        fun getInstance(): AuthRepository {
            if (instance == null) {
                instance = AuthRepository()
            }
            return instance!!
        }
    }
}