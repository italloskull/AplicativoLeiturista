package com.example.oaplicativo.util.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NavigationUtils {
    /**
     * Opens Google Maps for navigation to the specified coordinates.
     */
    fun openNavigation(context: Context, latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback: open in browser or any map app if Google Maps is not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"))
                context.startActivity(browserIntent)
            }
        } else {
            Toast.makeText(context, "Coordenadas não disponíveis para este cliente.", Toast.LENGTH_SHORT).show()
        }
    }
}