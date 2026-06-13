package com.somewhere.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: DropRepository
) : ViewModel() {

    data class SettingsUiState(
        val isClearing: Boolean = false,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun deleteAllDrops() {
        _uiState.value = _uiState.value.copy(isClearing = true, message = null)
        viewModelScope.launch {
            repository.deleteAllDrops()
            _uiState.value = _uiState.value.copy(
                isClearing = false,
                message = "All drops deleted"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
