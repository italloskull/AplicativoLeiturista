package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@androidx.compose.runtime.Immutable
@Serializable
data class EconomyUpdate(
    val id: String? = null,
    @SerialName("cidade_id") val cidadeId: String? = null,
    @SerialName("leiturista_id") val leituristaId: String? = null,
    
    @SerialName("numero_hd") val hdNumber: String,
    @SerialName("nome_edificio") val buildingName: String, // SÊNIOR FIX: Voltando para 'nome_edificio' conforme o erro do Supabase
    @SerialName("construtora") val constructionCompany: String,
    @SerialName("qtd_economias") val economiesCount: Int,
    @SerialName("qtd_pavimentos") val floorsCount: Int,
    @SerialName("medidor_energia") val electricityMeterNumber: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("adicionado_por") val addedBy: String? = null,
    @SerialName("criado_em") val createdAt: String? = null,
    val date: String? = null,
    @Transient val isSynced: Boolean = true // SÊNIOR FIX: Deve ser @Transient para o Supabase ignorar esta coluna no envio
)
