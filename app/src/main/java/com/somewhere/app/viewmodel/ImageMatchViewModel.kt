package com.somewhere.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.somewhere.app.data.repository.ImageMatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
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

    private var loadJob: Job? = null
    private var isProcessingFrame = false
    private var analysisStartedAtMillis = 0L
    private var analyzedFrameCount = 0
    private var bestScore = 0

    fun startLiveAnalysis(originalImageUrl: String) {
        loadJob?.cancel()
        _matchState.value = MatchState.LOADING_ORIGINAL
        _matchScore.value = null
        isProcessingFrame = false
        analyzedFrameCount = 0
        bestScore = 0
        loadJob = viewModelScope.launch {
            val success = repository.loadOriginalImage(originalImageUrl)
            if (success) {
                analysisStartedAtMillis = android.os.SystemClock.elapsedRealtime()
                _matchState.value = MatchState.ANALYZING
            } else {
                _matchScore.value = -1
                _matchState.value = MatchState.RESULT
            }
        }
    }

    fun processLiveFrame(bitmap: Bitmap) {
        // Only process frames if we are actively analyzing
        if (_matchState.value != MatchState.ANALYZING || isProcessingFrame) return

        isProcessingFrame = true
        viewModelScope.launch {
            try {
                val score = repository.computeLiveScore(bitmap)
                if (_matchState.value == MatchState.ANALYZING) {
                    analyzedFrameCount++
                    if (score != -1) {
                        bestScore = maxOf(bestScore, score)
                        _matchScore.value = score
                    }
                    val analysisTimedOut =
                        android.os.SystemClock.elapsedRealtime() - analysisStartedAtMillis >= 15_000L
                    if (score >= 80 || analysisTimedOut || analyzedFrameCount >= 120) {
                        _matchScore.value = bestScore
                        _matchState.value = MatchState.RESULT
                    }
                }
            } finally {
                isProcessingFrame = false
            }
        }
    }

    fun reset() {
        loadJob?.cancel()
        _matchState.value = MatchState.IDLE
        _matchScore.value = null
        isProcessingFrame = false
    }
}
