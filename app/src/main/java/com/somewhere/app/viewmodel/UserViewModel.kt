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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun saveProfile(name: String, gender: String, avatarUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                var avatarUrl: String? = null
                
                if (avatarUri != null) {
                    val bytes = context.contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val fileName = "${UUID.randomUUID()}.jpg"
                        val bucket = SupabaseManager.client.storage["avatars"]
                        bucket.upload(fileName, bytes)
                        avatarUrl = bucket.publicUrl(fileName)
                    }
                } else {
                    // Fallback to default avatar based on gender
                    avatarUrl = if (gender.lowercase() == "female") {
                        "default_female"
                    } else {
                        "default_male"
                    }
                }

                SupabaseManager.client.auth.updateUser {
                    data = buildJsonObject {
                        put("name", name.trim())
                        put("gender", gender)
                        if (avatarUrl != null) {
                            put("avatar_url", avatarUrl)
                        }
                    }
                }
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
