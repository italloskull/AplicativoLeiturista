package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val username: String? = null,
    val cargo: String = "usuário",
    @SerialName("cidade_id")
    val cidadeId: String? = null
) {
    val isAdmin: Boolean get() = cargo.lowercase() == "administrador"
}