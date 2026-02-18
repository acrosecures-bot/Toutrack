package com.example.tourist_main

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseManager {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://enjcclpjmeaceohqqiki.supabase.co",
        supabaseKey = "sb_publishable_s7QUT4RKga6lyEFJar5pCg_j8Qp37qI"
    ) {
        install(Storage)
    }
}
