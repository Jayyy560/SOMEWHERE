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

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.somewhere.app.data.model.Drop

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: DropRepository
) : ViewModel() {

    val myDrops: StateFlow<List<Drop>> = repository.allDrops
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    data class SettingsUiState(
        val isClearing: Boolean = false,
        val message: String? = null
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    init {
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        _blockedUsers.value = repository.getBlockedUsers()
    }

    fun unblockUser(authorName: String) {
        repository.unblockUser(authorName)
        loadBlockedUsers()
    }

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
