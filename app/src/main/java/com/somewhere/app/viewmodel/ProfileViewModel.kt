package com.somewhere.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
            repository.syncMyDrops()
        }
        loadUnlockedDrops()
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
            com.somewhere.app.data.remote.SupabaseManager.client.auth.signOut()
            onComplete()
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllDrops()
            com.somewhere.app.data.remote.SupabaseManager.client.auth.signOut()
            onComplete()
        }
    }
}
