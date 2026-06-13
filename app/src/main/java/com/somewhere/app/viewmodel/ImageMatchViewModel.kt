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
    LOADING_ORIGINAL,
    ANALYZING,
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

    fun startLiveAnalysis(originalImageUrl: String) {
        _matchState.value = MatchState.LOADING_ORIGINAL
        _matchScore.value = null
        viewModelScope.launch {
            val success = repository.loadOriginalImage(originalImageUrl)
            if (success) {
                _matchState.value = MatchState.ANALYZING
            } else {
                _matchScore.value = -1
                _matchState.value = MatchState.RESULT
            }
        }
    }

    fun processLiveFrame(bitmap: Bitmap) {
        // Only process frames if we are actively analyzing
        if (_matchState.value != MatchState.ANALYZING) return

        viewModelScope.launch {
            val score = repository.computeLiveScore(bitmap)
            if (score != -1) {
                _matchScore.value = score
                if (score >= 80) {
                    _matchState.value = MatchState.RESULT
                }
            }
        }
    }

    fun reset() {
        _matchState.value = MatchState.IDLE
        _matchScore.value = null
    }
}
