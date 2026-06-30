package com.somewhere.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.remote.SupabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

data class NotificationItem(val id: String, val title: String, val message: String, val time: String)

private fun getRelativeTime(timestampStr: String): String {
    return try {
        val instant = java.time.Instant.parse(timestampStr)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        
        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toHours() < 1 -> "${duration.toMinutes()}m ago"
            duration.toDays() < 1 -> "${duration.toHours()}h ago"
            duration.toDays() < 7 -> "${duration.toDays()}d ago"
            else -> "${duration.toDays() / 7}w ago"
        }
    } catch (e: Exception) {
        "Just now"
    }
}

@HiltViewModel
class UserViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val prefs = context.getSharedPreferences("somewhere_prefs", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _hasNotifications = MutableStateFlow(false)
    val hasNotifications: StateFlow<Boolean> = _hasNotifications.asStateFlow()

    init {
        viewModelScope.launch {
            SupabaseManager.client.auth.sessionStatus.collect { status ->
                if (status is io.github.jan.supabase.gotrue.SessionStatus.Authenticated) {
                    loadNotifications()
                    launch { startRealtimeNotifications() }
                } else {
                    _notifications.value = emptyList()
                    _hasNotifications.value = false
                }
            }
        }
    }

    private suspend fun startRealtimeNotifications() {
        try {
            val user = SupabaseManager.client.auth.currentUserOrNull() ?: return
            val channel = SupabaseManager.client.channel("notifications-${user.id}")
            channel.subscribe()
            channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction>("public") {
                table = "notifications"
                filter = "user_id=eq.${user.id}"
            }.collect {
                loadNotifications()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val user = SupabaseManager.client.auth.currentUserOrNull()
            if (user == null) {
                _notifications.value = emptyList()
                return@launch
            }
            try {
                val remoteNotifs = SupabaseManager.client.postgrest["notifications"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeList<com.somewhere.app.data.remote.NotificationItem>()
                    // Filter out any lingering self-likes/comments
                    .filter { it.actorName != user.userMetadata?.get("name")?.jsonPrimitive?.content }
                    .sortedByDescending { it.createdAt }

                val mappedNotifs = remoteNotifs.map { remote ->
                    NotificationItem(
                        id = remote.id ?: java.util.UUID.randomUUID().toString(),
                        title = if (remote.type == "like") "New Like" else if (remote.type == "comment") "New Comment" else "System",
                        message = remote.message,
                        time = getRelativeTime(remote.createdAt ?: "")
                    )
                }

                val baseNotifs = if (prefs.getBoolean("welcome_cleared", false)) {
                    emptyList()
                } else {
                    listOf(
                        NotificationItem("1", "Welcome to Somewhere!", "Start exploring and dropping memories.", "Just now")
                    )
                }

                val combined = baseNotifs + mappedNotifs
                _notifications.value = combined
                // Only show the purple notification drop if there is at least one UNREAD notification
                _hasNotifications.value = remoteNotifs.any { !it.isRead } || (!prefs.getBoolean("welcome_cleared", false) && baseNotifs.isNotEmpty())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                if (user != null) {
                    SupabaseManager.client.postgrest["notifications"].delete {
                        filter { eq("user_id", user.id) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _notifications.value = emptyList()
            _hasNotifications.value = false
            prefs.edit().putBoolean("welcome_cleared", true).apply()
        }
    }

    fun markNotificationsAsSeen() {
        viewModelScope.launch {
            _hasNotifications.value = false
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                if (user != null) {
                    SupabaseManager.client.postgrest["notifications"].update({
                        set("is_read", true)
                    }) {
                        filter { 
                            eq("user_id", user.id)
                            eq("is_read", false)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeNotification(id: String) {
        viewModelScope.launch {
            if (id != "1") {
                try {
                    SupabaseManager.client.postgrest["notifications"].delete {
                        filter { eq("id", id) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val remaining = _notifications.value.filter { it.id != id }
            _notifications.value = remaining
            if (id == "1") {
                prefs.edit().putBoolean("welcome_cleared", true).apply()
            }
            if (remaining.isEmpty()) {
                _hasNotifications.value = false
            }
        }
    }

    fun saveProfile(name: String, gender: String, avatarUri: Uri?, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                var avatarUrl: String? = null
                
                val currentUser = SupabaseManager.client.auth.currentUserOrNull()
                val userId = currentUser?.id ?: UUID.randomUUID().toString()

                if (avatarUri != null) {
                    val bytes = context.contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val fileName = "$userId/avatar.jpg"
                        val bucket = SupabaseManager.client.storage["avatars"]
                        bucket.upload(fileName, bytes, upsert = true)
                        avatarUrl = bucket.publicUrl(fileName)
                    }
                } else {
                    // Keep existing avatar unless it's a default, then update based on gender
                    val currentAvatar = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
                    if (currentAvatar == null || currentAvatar.toString().startsWith("default_")) {
                        avatarUrl = if (gender.lowercase() == "female") {
                            "default_female"
                        } else {
                            "default_male"
                        }
                    } else {
                        avatarUrl = currentAvatar
                    }
                }

                SupabaseManager.client.auth.updateUser {
                    data = buildJsonObject {
                        put("name", name.trim())
                        put("gender", gender)
                        if (avatarUrl != null) {
                            put("avatar_url", avatarUrl.toString())
                        }
                    }
                }
                
                // Update the local cache of the session with the new name/avatar
                SupabaseManager.saveCurrentSessionToStore()
                
                onComplete(null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(e.message ?: "Failed to save profile")
            } finally {
                _isSaving.value = false
            }
        }
    }
}
