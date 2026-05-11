package com.example.oaplicativo.model

import kotlinx.serialization.Serializable

@Serializable
data class Cidade(
    val id: String,
    val nome: String
)
