package com.example.oaplicativo.data.remote.osm

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface NominatimService {
    @Headers("User-Agent: RecadastreIA-App-Brasil")
    @GET("reverse?format=json&addressdetails=1")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<NominatimResponse>
}
