package com.somewhere.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.jan.supabase.gotrue.auth

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: DropRepository
) : ViewModel() {

    val myDrops: StateFlow<List<Drop>> = repository.allDrops
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _unlockedDrops = kotlinx.coroutines.flow.MutableStateFlow<List<Drop>>(emptyList())
    val unlockedDrops: StateFlow<List<Drop>> = _unlockedDrops

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            com.somewhere.app.data.remote.SupabaseManager.client.auth.sessionStatus.collect {
                repository.syncMyDrops()
                loadUnlockedDrops()
            }
        }
    }

    private fun loadUnlockedDrops() {
        viewModelScope.launch {
            _isLoading.value = true
            _unlockedDrops.value = repository.getUnlockedDrops()
            _isLoading.value = false
        }
    }

    fun refreshUnlockedDrops() {
        viewModelScope.launch {
            _unlockedDrops.value = repository.getUnlockedDrops()
        }
    }

    fun deleteDrop(drop: Drop) {
        viewModelScope.launch {
            repository.deleteDrop(drop)
        }
    }

    fun logOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearLocalCache()
            val userId = com.somewhere.app.data.remote.SupabaseManager.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                com.somewhere.app.data.local.AccountStore.removeAccount(userId)
            }
            com.somewhere.app.data.remote.SupabaseManager.client.auth.signOut()
            onComplete()
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllDrops()
            val userId = com.somewhere.app.data.remote.SupabaseManager.client.auth.currentUserOrNull()?.id
            if (userId != null) {
                com.somewhere.app.data.local.AccountStore.removeAccount(userId)
            }
            try {
                // Call Supabase RPC to securely delete the auth.users record
                com.somewhere.app.data.remote.SupabaseManager.client.postgrest.rpc("delete_user")
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to delete remote account: ${e.message}")
            }
            com.somewhere.app.data.remote.SupabaseManager.client.auth.signOut()
            onComplete()
        }
    }

    fun blockUser(authorName: String) {
        viewModelScope.launch {
            repository.blockUser(authorName)
            refreshUnlockedDrops() // Refresh drops to remove blocked user's drops if needed
        }
    }
}
