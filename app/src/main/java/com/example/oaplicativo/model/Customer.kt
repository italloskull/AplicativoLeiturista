package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String? = null,
    val name: String? = null,
    @SerialName("registration_number")
    val registrationNumber: String? = null,
    @SerialName("registration_digit")
    val registrationDigit: String? = null,
    val email: String? = null,
    val landline: String? = null,
    @SerialName("cell_phone")
    val cellPhone: String? = null,
    @SerialName("is_standard_measurement_box")
    val isStandardMeasurementBox: Boolean? = null,
    @SerialName("is_standardized_seals")
    val isStandardizedSeals: Boolean? = null,
    @SerialName("is_hd_accessible")
    val isHdAccessible: Boolean? = null,
    @SerialName("is_vacationer")
    val isVacationer: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("location_status")
    val locationStatus: String? = null,
    @SerialName("economies_count")
    val economiesCount: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    
    // Novos campos de Auditoria
    @SerialName("added_by")
    val addedBy: String? = null,
    @SerialName("captured_at")
    val capturedAt: String? = null,
    @SerialName("synced_at")
    val syncedAt: String? = null,
    
    // Novo campo de Data Simplificada (yyyy/MM/dd)
    @SerialName("date")
    val date: String? = null,

    @kotlinx.serialization.Transient
    val isSynced: Boolean = true
)