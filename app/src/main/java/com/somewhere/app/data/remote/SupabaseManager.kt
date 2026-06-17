package com.somewhere.app.data.remote

import com.somewhere.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.gotrue.auth

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SupabaseManager {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject { 
                put("name", name) 
                put("has_strong_password", true)
            }
        }
    }

    suspend fun resetPasswordForEmail(email: String, redirectUrl: String = "somewhere://reset") {
        client.auth.resetPasswordForEmail(email, redirectUrl)
    }

    suspend fun updatePassword(password: String) {
        client.auth.updateUser {
            this.password = password
            this.data = buildJsonObject { put("has_strong_password", true) }
        }
    }
}
