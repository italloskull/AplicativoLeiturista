@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.data

import com.example.oaplicativo.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * CLIENTE SUPABASE (SÊNIOR)
 */
object SupabaseClient {
    private const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            explicitNulls = true // SÊNIOR FIX: Garante que campos NULL sejam enviados explicitamente
            encodeDefaults = false
        })
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Functions)
        install(Realtime)
    }
}
