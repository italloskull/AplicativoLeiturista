package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * EconomyUpdate: Modelo de Dados Sincronizado com o Supabase (Versão Certificada)
 * SÊNIOR FIX: Campos numéricos marcados como opcionais (Int?) para permitir NULL no banco de dados.
 */
@Serializable
data class EconomyUpdate(
    val id: String? = null,
    @SerialName("leiturista_id") val leituristaId: String? = null,
    @SerialName("numero_hd") val hdNumber: String? = null,
    @SerialName("nome_edificio") val buildingName: String? = null,
    @SerialName("construtora") val constructionCompany: String? = null,
    @SerialName("qtd_economias") val economiesCount: Int? = null,
    @SerialName("qtd_pavimentos") val floorsCount: Int? = null,
    @SerialName("medidor_energia") val electricityMeterNumber: String? = null,
    
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("adicionado_por") val addedBy: String? = null,
    @SerialName("data") val date: String? = null,
    
    @SerialName("cidade") val cidade: String? = null,
    @SerialName("cidade_id") val cidadeId: String? = null,
    @SerialName("grupo_sugerido") val grupoSugerido: String? = null,
    @SerialName("rota_sugerida") val rotaSugerida: String? = null,
    
    @Transient val isSynced: Boolean = true
)
