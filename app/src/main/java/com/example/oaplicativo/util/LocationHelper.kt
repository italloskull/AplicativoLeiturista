package com.example.oaplicativo.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val sharedPrefs = context.getSharedPreferences("location_cache", Context.MODE_PRIVATE)

    /**
     * 🔥 FIX DE DEPURAÇÃO: Adicionado Timeout de 10s.
     * Evita que o app trave infinitamente se não houver sinal de satélite.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return withTimeoutOrNull(10000L) { // Timeout de segurança de 10 segundos
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            cacheLocation(location)
                            continuation.resume(location)
                        } else {
                            continuation.resume(getCachedLocation())
                        }
                    }.addOnFailureListener {
                        continuation.resume(getCachedLocation())
                    }
                } catch (e: Exception) {
                    Log.e("LocationHelper", "Erro ao obter GPS: ${e.message}")
                    continuation.resume(getCachedLocation())
                }
            }
        } ?: getCachedLocation() // Se der timeout, retorna o cache
    }

    private fun cacheLocation(location: Location) {
        sharedPrefs.edit().apply {
            putFloat("last_lat", location.latitude.toFloat())
            putFloat("last_long", location.longitude.toFloat())
            putLong("last_time", System.currentTimeMillis())
            apply()
        }
    }

    fun getCachedLocation(): Location? {
        val lat = sharedPrefs.getFloat("last_lat", 0f).toDouble()
        val lng = sharedPrefs.getFloat("last_long", 0f).toDouble()
        val timeStamp = sharedPrefs.getLong("last_time", 0L)

        if (lat == 0.0 && lng == 0.0) return null

        val cachedLoc = Location("cached")
        cachedLoc.latitude = lat
        cachedLoc.longitude = lng
        cachedLoc.time = timeStamp
        return cachedLoc
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun formatDistance(meters: Float): String {
        return if (meters < 1000) {
            "${meters.toInt()}m"
        } else {
            "%.1f km".format(meters / 1000)
        }
    }
}
