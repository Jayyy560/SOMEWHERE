package com.somewhere.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.BuildConfig
import com.somewhere.app.data.remote.SupabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    enum class Mode {
        SIGN_IN,
        SIGN_UP
    }

    data class AuthUiState(
        val email: String = "",
        val password: String = "",
        val mode: Mode = Mode.SIGN_IN,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun toggleMode() {
        val next = if (_uiState.value.mode == Mode.SIGN_IN) Mode.SIGN_UP else Mode.SIGN_IN
        _uiState.value = _uiState.value.copy(mode = next, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password required")
            return
        }

        val url = BuildConfig.SUPABASE_URL.trim()
        if (url.isBlank() || url.contains("localhost", ignoreCase = true)) {
            _uiState.value = state.copy(
                errorMessage = "Supabase URL is not set correctly. Update local.properties and rebuild."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = runCatching {
                if (state.mode == Mode.SIGN_IN) {
                    SupabaseManager.signInWithEmail(state.email.trim(), state.password)
                } else {
                    SupabaseManager.signUpWithEmail(state.email.trim(), state.password)
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
