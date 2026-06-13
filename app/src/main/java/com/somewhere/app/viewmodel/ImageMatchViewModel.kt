package com.somewhere.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.repository.ImageMatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MatchState {
    IDLE,
    CAPTURING,
    PROCESSING,
    RESULT
}

@HiltViewModel
class ImageMatchViewModel @Inject constructor(
    private val repository: ImageMatchRepository
) : ViewModel() {

    private val _matchState = MutableStateFlow(MatchState.IDLE)
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _matchScore = MutableStateFlow<Int?>(null)
    val matchScore: StateFlow<Int?> = _matchScore.asStateFlow()

    fun startCapture() {
        _matchState.value = MatchState.CAPTURING
        _matchScore.value = null
    }

    fun processCapturedImage(originalImageUrl: String, capturedBitmap: Bitmap) {
        _matchState.value = MatchState.PROCESSING
        viewModelScope.launch {
            val score = repository.computeMatchScore(originalImageUrl, capturedBitmap)
            _matchScore.value = score
            _matchState.value = MatchState.RESULT
        }
    }

    fun reset() {
        _matchState.value = MatchState.IDLE
        _matchScore.value = null
    }
}
