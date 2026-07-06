package com.example.oaplicativo.domain.repository

import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.model.Cidade
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserProfile: StateFlow<UserProfile?>
    suspend fun loadProfileFromCache(context: android.content.Context)
    suspend fun login(identifier: String, pass: String)
    suspend fun fetchProfile()
    suspend fun setLocalProfile(profile: UserProfile?)
    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        username: String,
        role: String,
        cidades: List<String>
    )
    suspend fun logout()
    suspend fun getUserCities(): List<Cidade> // SÊNIOR FIX: Recupera cidades autorizadas via usuario_cidades
}
