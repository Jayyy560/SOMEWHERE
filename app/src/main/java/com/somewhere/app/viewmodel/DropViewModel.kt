package com.somewhere.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

/**
 * ViewModel for the Drop screen.
 * Manages the captured photo URI, text input, and save operation.
 */
@HiltViewModel
class DropViewModel @Inject constructor(
    private val repository: DropRepository
) : ViewModel() {

    data class DropUiState(
        val capturedImageUri: Uri? = null,
        val recordedAudioUri: Uri? = null,
        val text: String = "",
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val error: String? = null,
        val isMoment: Boolean = false,
        val durationMs: Long = 3600000L, // 1 hour default
        val customDurationLabel: String? = null,
        val category: String = "Story",
        val isAnonymous: Boolean = false
    )

    private val _uiState = MutableStateFlow(DropUiState())
    val uiState: StateFlow<DropUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(uri: Uri) {
        _uiState.update { it.copy(capturedImageUri = uri) }
    }

    fun onTextChanged(text: String) {
        if (text.length <= 120) {
            _uiState.update { it.copy(text = text) }
        }
    }

    fun setDropType(isMoment: Boolean) {
        _uiState.update { it.copy(isMoment = isMoment) }
    }

    fun setDuration(durationMs: Long, customLabel: String? = null) {
        _uiState.update { it.copy(durationMs = durationMs, customDurationLabel = customLabel) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun setAnonymous(isAnonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = isAnonymous) }
    }

    fun generateAiDrop(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("ai_drops.json").bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                val drops = List(jsonArray.length()) { jsonArray.getString(it) }
                val randomDrop = drops.random()
                _uiState.update { it.copy(text = randomDrop) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load AI drops") }
            }
        }
    }

    fun onAudioCaptured(uri: Uri) {
        _uiState.update { it.copy(recordedAudioUri = uri) }
    }

    fun clearAudio() {
        val current = _uiState.value.recordedAudioUri ?: return
        viewModelScope.launch {
            repository.deleteLocalAudio(current.toString())
        }
        _uiState.update { it.copy(recordedAudioUri = null) }
    }

    fun saveDrop(latitude: Double, longitude: Double) {
        val state = _uiState.value
        val imageUri = state.capturedImageUri ?: return
        if (state.text.isBlank()) return

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val expiresAt = if (state.isMoment) System.currentTimeMillis() + state.durationMs else null
                repository.saveDrop(
                    text = state.text,
                    imagePath = imageUri.toString(),
                    audioPath = state.recordedAudioUri?.toString(),
                    latitude = latitude,
                    longitude = longitude,
                    expiresAt = expiresAt,
                    isAnonymous = state.isAnonymous,
                    category = state.category
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save"
                ) }
            }
        }
    }

    fun reset() {
        val previous = _uiState.value
        _uiState.update { DropUiState() }
        previous.capturedImageUri?.let { uri ->
            viewModelScope.launch {
                if (!previous.isSaved) {
                    repository.deleteLocalImage(uri.toString())
                }
            }
        }
        previous.recordedAudioUri?.let { uri ->
            viewModelScope.launch {
                if (!previous.isSaved) {
                    repository.deleteLocalAudio(uri.toString())
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
