package com.example.oaplicativo.data

import com.example.oaplicativo.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseClient {
    // Agora lendo do BuildConfig gerado pelo Gradle
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // Usar o motor OkHttp que suporta WebSockets (necessário para Realtime)
        httpEngine = OkHttp.create()

        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Functions)
        install(Realtime)
    }
}