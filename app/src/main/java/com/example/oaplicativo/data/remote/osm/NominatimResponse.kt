package com.example.oaplicativo.data.remote.osm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponse(
    @SerialName("display_name") val displayName: String? = null,
    val address: OsmAddress? = null
)

@Serializable
data class OsmAddress(
    val road: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null
)
