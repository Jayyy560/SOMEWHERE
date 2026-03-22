package com.somewhere.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        val text: String = "",
        val isSaving: Boolean = false,
        val isSaved: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(DropUiState())
    val uiState: StateFlow<DropUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(uri: Uri) {
        _uiState.value = _uiState.value.copy(capturedImageUri = uri)
    }

    fun onTextChanged(text: String) {
        if (text.length <= 120) {
            _uiState.value = _uiState.value.copy(text = text)
        }
    }

    fun saveDrop(latitude: Double, longitude: Double) {
        val state = _uiState.value
        val imageUri = state.capturedImageUri ?: return
        if (state.text.isBlank()) return

        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                repository.saveDrop(
                    text = state.text,
                    imagePath = imageUri.toString(),
                    latitude = latitude,
                    longitude = longitude
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save"
                )
            }
        }
    }

    fun reset() {
        val previous = _uiState.value
        _uiState.value = DropUiState()
        previous.capturedImageUri?.let { uri ->
            viewModelScope.launch {
                if (!previous.isSaved) {
                    repository.deleteLocalImage(uri.toString())
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
