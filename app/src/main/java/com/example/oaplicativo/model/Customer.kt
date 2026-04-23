package com.example.oaplicativo.model

data class Customer(
    val id: String = "",
    val name: String = "",
    val registrationNumber: String = "",
    val email: String = "",
    val landline: String = "",
    val cellPhone: String = "",
    val isStandardMeasurementBox: Boolean = false,
    val isStandardizedSeals: Boolean = false,
    val isHdAccessible: Boolean = false,
    val isVacationer: Boolean = false
)