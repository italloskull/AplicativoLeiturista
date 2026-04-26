package com.example.oaplicativo.domain.repository

import com.example.oaplicativo.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserProfile: StateFlow<UserProfile?>
    suspend fun login(identifier: String, password: String)
    suspend fun fetchProfile()
    suspend fun registerUser(name: String, email: String, password: String, sector: String, role: String)
    suspend fun logout()
}