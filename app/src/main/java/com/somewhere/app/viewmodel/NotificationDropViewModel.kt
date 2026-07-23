package com.somewhere.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationDropUiState(
    val drop: Drop? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationDropViewModel @Inject constructor(
    private val repository: DropRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationDropUiState())
    val uiState: StateFlow<NotificationDropUiState> = _uiState.asStateFlow()

    fun load(dropId: String) {
        viewModelScope.launch {
            _uiState.value = NotificationDropUiState(isLoading = true)
            val drop = repository.getDropById(dropId)
            _uiState.value = if (drop != null) {
                NotificationDropUiState(drop = drop)
            } else {
                NotificationDropUiState(error = "This drop is unavailable or was removed.")
            }
        }
    }

    fun delete(drop: Drop) {
        viewModelScope.launch { repository.deleteDrop(drop) }
    }

    fun report(dropId: String) {
        repository.reportDrop(dropId)
    }

    fun block(authorName: String?) {
        authorName?.let(repository::blockUser)
    }
}
