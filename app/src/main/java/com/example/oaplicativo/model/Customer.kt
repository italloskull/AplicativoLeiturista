package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String? = null,
    val name: String? = null,
    @SerialName("registration_number") val registrationNumber: String? = null,
    @SerialName("registration_digit") val registrationDigit: String? = null,
    val email: String? = null,
    val landline: String? = null,
    @SerialName("cell_phone") val cellPhone: String? = null,
    
    @SerialName("is_standard_measurement_box") val isStandardMeasurementBox: Boolean? = null,
    @SerialName("is_standardized_seals") val isStandardizedSeals: Boolean? = null,
    @SerialName("is_hd_accessible") val isHdAccessible: Boolean? = null,
    @SerialName("is_vacationer") val isVacationer: Boolean? = null,
    
    @SerialName("possui_piscina") val possuiPiscina: Boolean? = null,
    @SerialName("possui_caixa_agua") val possuiCaixaAgua: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("location_status") val locationStatus: String? = null,
    @SerialName("economies_count") val economiesCount: Int? = null,
    
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("added_by") val addedBy: String? = null,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("synced_at") val syncedAt: String? = null,
    val date: String? = null,

    // NOVO: Qualidade Automática
    @SerialName("qualidade_cadastro") val quality: String? = null,

    // NOVO: Campos Multi-Papel (Entrevistado)
    @SerialName("entrevistado_nome") val entrevistadoNome: String? = null,
    @SerialName("entrevistado_cpf") val entrevistadoCpf: String? = null,
    @SerialName("entrevistado_mae") val entrevistadoMae: String? = null,
    @SerialName("entrevistado_nascimento") val entrevistadoNascimento: String? = null,
    @SerialName("entrevistado_sexo") val entrevistadoSexo: String? = null,
    @SerialName("entrevistado_apresentou_doc") val entrevistadoApresentouDoc: Boolean? = null,
    @SerialName("entrevistado_qual_doc") val entrevistadoQualDoc: String? = null,

    // NOVO: Campos Multi-Papel (Proprietário)
    @SerialName("proprietario_nome") val proprietarioNome: String? = null,
    @SerialName("proprietario_cpf") val proprietarioCpf: String? = null,
    @SerialName("proprietario_mae") val proprietarioMae: String? = null,
    @SerialName("proprietario_nascimento") val proprietarioNascimento: String? = null,
    @SerialName("proprietario_sexo") val proprietarioSexo: String? = null,
    @SerialName("proprietario_apresentou_doc") val proprietarioApresentouDoc: Boolean? = null,
    @SerialName("proprietario_qual_doc") val proprietarioQualDoc: String? = null,

    // NOVO: Campos Multi-Papel (Locatário)
    @SerialName("locatario_nome") val locatarioNome: String? = null,
    @SerialName("locatario_cpf") val locatarioCpf: String? = null,
    @SerialName("locatario_mae") val locatarioMae: String? = null,
    @SerialName("locatario_nascimento") val locatarioNascimento: String? = null,
    @SerialName("locatario_sexo") val locatarioSexo: String? = null,
    @SerialName("locatario_apresentou_doc") val locatarioApresentouDoc: Boolean? = null,
    @SerialName("locatario_qual_doc") val locatarioQualDoc: String? = null,

    val isSynced: Boolean = true
)
