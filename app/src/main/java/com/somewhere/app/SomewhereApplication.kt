package com.somewhere.app

import android.app.Application
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonPrimitive

@HiltAndroidApp
class SomewhereApplication : Application() {

    @Inject
    lateinit var repository: DropRepository

    override fun onCreate() {
        super.onCreate()
        com.somewhere.app.data.local.AccountStore.init(this)
        
        CoroutineScope(Dispatchers.IO).launch {
            // Keep the active account in sync so we don't lose the refreshed tokens
            com.somewhere.app.data.remote.SupabaseManager.client.auth.sessionStatus.collect { status ->
                if (status is io.github.jan.supabase.gotrue.SessionStatus.Authenticated) {
                    com.somewhere.app.data.remote.SupabaseManager.saveCurrentSessionToStore()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            repository.cleanupOrphanMedia()
            setupNotificationListener()
        }
    }

    private suspend fun setupNotificationListener() {
        com.somewhere.app.util.NotificationHelper.createNotificationChannel(this)
        val channel = com.somewhere.app.data.remote.SupabaseManager.client.channel("realtime-notifications")
        channel.subscribe()
        channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>("public") {
            table = "notifications"
        }.collect { action ->
            val currentUser = com.somewhere.app.data.remote.SupabaseManager.client.auth.currentUserOrNull()
            if (currentUser != null) {
                val userId = action.record["user_id"]?.jsonPrimitive?.content
                if (userId == currentUser.id) {
                    val message = action.record["message"]?.jsonPrimitive?.content ?: "New activity on your drop!"
                    val title = if (action.record["type"]?.jsonPrimitive?.content == "like") "New Like" else "New Comment"
                    com.somewhere.app.util.NotificationHelper.showNotification(this@SomewhereApplication, title, message)
                }
            }
        }
    }
}
