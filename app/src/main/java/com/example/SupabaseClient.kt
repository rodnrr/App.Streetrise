package com.example

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    lateinit var client: SupabaseClient

    fun initialize(supabaseUrl: String, supabaseKey: String) {
        client = createSupabaseClient(supabaseUrl, supabaseKey) {
            install(Postgrest)
            install(Auth)
        }
    }
}
