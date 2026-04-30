package com.example.oaplicativo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EconomyUpdate(
    val id: String? = null,
    @SerialName("hd_number")
    val hdNumber: String,
    @SerialName("building_name")
    val buildingName: String,
    @SerialName("construction_company")
    val constructionCompany: String,
    @SerialName("economies_count")
    val economiesCount: Int,
    @SerialName("floors_count")
    val floorsCount: Int,
    @SerialName("electricity_meter_number")
    val electricityMeterNumber: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("user_id")
    val userId: String? = null
)