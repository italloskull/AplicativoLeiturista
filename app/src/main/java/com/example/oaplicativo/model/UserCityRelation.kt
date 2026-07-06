package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCityRelation(
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("cidade_id") val cidadeId: String
)
