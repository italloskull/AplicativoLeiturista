package com.example.oaplicativo.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    private const val SUPABASE_URL = "https://epykjmovelezxnqcawyh.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_PC15cfLyjWEYR0-oZnUWsA_TsfA7-n3"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}