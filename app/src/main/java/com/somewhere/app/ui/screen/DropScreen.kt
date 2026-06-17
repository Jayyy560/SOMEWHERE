package com.somewhere.app.ui.screen

import android.content.Context
import android.os.Build
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.view.MotionEvent
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.somewhere.app.ui.component.PermissionGate
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DropViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val MAX_AUDIO_RECORDING_MS = 20_000L
private const val MIN_AUDIO_RECORDING_MS = 600L

/**
 * Drop screen — capture a photo, write a message, mark this place.
 *
 * Flow:
 * 1. Fullscreen camera preview
 * 2. Capture button → freezes image
 * 3. Text input appears
 * 4. "Mark this place" → saves drop with current GPS
 */
@Composable
fun DropScreen(
    imageCapture: ImageCapture?,
    triggerCapture: Boolean,
    onCaptureTriggered: () -> Unit,
    onFocusTap: (Float, Float) -> Unit,
    onComplete: () -> Unit,
    onCreationStateChanged: (Boolean) -> Unit = {},
    viewModel: DropViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotionEnabled()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeRecordingUri by remember { mutableStateOf<Uri?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartedAtMillis by remember { mutableStateOf<Long?>(null) }
    var waveformSamples by remember { mutableStateOf(List(16) { 0.12f }) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    LaunchedEffect(uiState.capturedImageUri) {
        onCreationStateChanged(uiState.capturedImageUri != null)
    }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val stopRecordingSession: () -> Unit = {
        val savedUri = stopAudioRecording(recorder, activeRecordingUri)
        recorder = null
        activeRecordingUri = null
        recordingStartedAtMillis = null
        if (isRecording) {
            isRecording = false
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        savedUri?.let {
            viewModel.onAudioCaptured(it)
            waveformSamples = List(16) { 0.12f }
        } ?: run {
            scope.launch {
                snackbarHostState.showSnackbar("Recording failed, hold a little longer")
            }
        }
    }

    val discardRecordingSession: () -> Unit = {
        discardAudioRecording(recorder, activeRecordingUri)
        recorder = null
        activeRecordingUri = null
        recordingStartedAtMillis = null
        if (isRecording) {
            isRecording = false
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    // Navigate back after successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    LaunchedEffect(uiState.recordedAudioUri) {
        if (uiState.recordedAudioUri == null) {
            runCatching {
                previewPlayer?.stop()
                previewPlayer?.release()
            }
            previewPlayer = null
            isPreviewPlaying = false
        }
    }

    // imageCapture is now passed in as a parameter

    LaunchedEffect(triggerCapture) {
        if (triggerCapture && uiState.capturedImageUri == null) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            val file = File(context.cacheDir, "drop_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        viewModel.onPhotoCaptured(Uri.fromFile(file))
                    }
                    override fun onError(exception: ImageCaptureException) {
                        // ignore or log
                    }
                }
            )
            onCaptureTriggered()
        }
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 600),
        label = "dropFade"
    )
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(isRecording, recorder) {
        while (isRecording && recorder != null) {
            val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
            val normalized = (amplitude / 32767f).coerceIn(0.08f, 1f)
            waveformSamples = (waveformSamples + normalized).takeLast(16)
            delay(75)
        }
    }

    LaunchedEffect(isRecording, recordingStartedAtMillis) {
        while (isRecording && recordingStartedAtMillis != null) {
            val elapsed = System.currentTimeMillis() - (recordingStartedAtMillis ?: 0L)
            if (elapsed >= MAX_AUDIO_RECORDING_MS) {
                snackbarHostState.showSnackbar("Audio limited to 20s")
                stopRecordingSession()
                break
            }
            delay(120)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.release() }
            runCatching {
                previewPlayer?.stop()
                previewPlayer?.release()
            }
            isPreviewPlaying = false
        }
    }
    
    var focusPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var focusTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) {
            kotlinx.coroutines.delay(1000)
            focusPoint = null
        }
    }

    PermissionGate(
        title = "Camera + Location + Mic",
        description = "Capture a photo, voice note, and attach them to this exact place.",
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.RECORD_AUDIO
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Do not apply background color so the camera shines through
                .alpha(contentAlpha)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            focusPoint = offset
                            focusTrigger++
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            onFocusTap(offset.x / width, offset.y / height)
                        }
                    )
                }
        ) {
            // Visual Focus Indicator
            focusPoint?.let { point ->
                val scale = remember(focusTrigger) { androidx.compose.animation.core.Animatable(1.5f) }
                val alpha = remember(focusTrigger) { androidx.compose.animation.core.Animatable(1f) }
                
                LaunchedEffect(focusTrigger) {
                    launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        kotlinx.coroutines.delay(500)
                        alpha.animateTo(0f, androidx.compose.animation.core.tween(500))
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = (point.x / LocalContext.current.resources.displayMetrics.density).dp - 36.dp,
                            y = (point.y / LocalContext.current.resources.displayMetrics.density).dp - 36.dp
                        )
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            this.alpha = alpha.value
                        }
                        .border(
                            width = 1.5.dp,
                            color = SomewhereColors.GlowAccent,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                ) {
                    // Small center dot
                    Box(modifier = Modifier.size(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(SomewhereColors.GlowAccent).align(Alignment.Center))
                }
            }

            // Camera preview is now rendered underneath this screen in MainPagerScreen
            if (uiState.capturedImageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uiState.capturedImageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Captured photo",
                    alignment = Alignment.TopCenter,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom panel
            val bottomPadding = if (uiState.capturedImageUri == null) 124.dp else 24.dp
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(SomewhereColors.Background.copy(alpha = 0.92f))
                    // Add extra 100dp bottom padding to sit above the FloatingBottomNav ONLY when nav is visible
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = bottomPadding)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.capturedImageUri == null) {
                    // Capture button
                    SomewhereButton(
                        text = "Capture",
                        onClick = {
                            takePhoto(context, imageCapture) { uri ->
                                viewModel.onPhotoCaptured(uri)
                            }
                        }
                    )
                } else {
                    // Top Row: Text Input & Audio Record Button side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BasicTextField(
                            value = uiState.text,
                            onValueChange = viewModel::onTextChanged,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp)) // Rounder corners for modern look
                                .background(SomewhereColors.Card)
                                .border(0.5.dp, SomewhereColors.CardBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = SomewhereColors.TextPrimary
                            ),
                            cursorBrush = SolidColor(SomewhereColors.AccentSubtle),
                            maxLines = 3,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.text.isEmpty()) {
                                        Text(
                                            text = "What happened here?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SomewhereColors.TextMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Audio Record Button
                        HoldToRecordButton(
                            isRecording = isRecording,
                            onPressStart = {
                                val result = startAudioRecording(context)
                                if (result != null) {
                                    recorder = result.first
                                    activeRecordingUri = result.second
                                    isRecording = true
                                    recordingStartedAtMillis = System.currentTimeMillis()
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    true
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Microphone unavailable") }
                                    false
                                }
                            },
                            onPressEnd = { heldMs ->
                                if (heldMs < MIN_AUDIO_RECORDING_MS) {
                                    discardRecordingSession()
                                    scope.launch { snackbarHostState.showSnackbar("Hold to record") }
                                } else {
                                    stopRecordingSession()
                                }
                            }
                        )
                    }

                    // Auto-write & Char count & Audio playback
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.generateAiDrop(context) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = "Auto-write",
                                style = MaterialTheme.typography.labelMedium,
                                color = SomewhereColors.GlowAccent
                            )
                        }

                        if (isRecording) {
                            WaveformBars(samples = waveformSamples)
                        } else if (uiState.recordedAudioUri != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedWaveform(active = isPreviewPlaying)
                                IconButton(
                                    onClick = {
                                        if (previewPlayer?.isPlaying == true) {
                                            previewPlayer?.pause()
                                            isPreviewPlaying = false
                                        } else if (previewPlayer != null) {
                                            runCatching {
                                                previewPlayer?.start()
                                                isPreviewPlaying = true
                                            }
                                        } else {
                                            previewPlayer = playAudio(context, uiState.recordedAudioUri!!) {
                                                isPreviewPlaying = false
                                                previewPlayer = null
                                            }
                                            isPreviewPlaying = previewPlayer != null
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = SomewhereColors.TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        runCatching {
                                            previewPlayer?.stop()
                                            previewPlayer?.release()
                                        }
                                        previewPlayer = null
                                        isPreviewPlaying = false
                                        viewModel.clearAudio()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear audio",
                                        tint = SomewhereColors.TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "${uiState.text.length}/120",
                                style = MaterialTheme.typography.labelSmall,
                                color = SomewhereColors.TextSecondary
                            )
                        }
                    }

                    // Options Row: Type & Ghost Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (!uiState.isMoment) SomewhereColors.GlowAccent else SomewhereColors.GlassBackground,
                                shape = CircleShape,
                                onClick = { viewModel.setDropType(false) }
                            ) {
                                Text(
                                    "Permanent",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!uiState.isMoment) SomewhereColors.Background else SomewhereColors.TextPrimary
                                )
                            }
                            Surface(
                                color = if (uiState.isMoment) SomewhereColors.GlowAccent else SomewhereColors.GlassBackground,
                                shape = CircleShape,
                                onClick = { viewModel.setDropType(true) }
                            ) {
                                Text(
                                    "Moment",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isMoment) SomewhereColors.Background else SomewhereColors.TextPrimary
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ghost", style = MaterialTheme.typography.labelSmall, color = if (uiState.isAnonymous) SomewhereColors.GlowAccent else SomewhereColors.TextMuted)
                            Switch(
                                checked = uiState.isAnonymous,
                                onCheckedChange = { viewModel.setAnonymous(it) },
                                modifier = Modifier.scale(0.8f).padding(start = 4.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SomewhereColors.Background,
                                    checkedTrackColor = SomewhereColors.GlowAccent
                                )
                            )
                        }
                    }

                    if (uiState.isMoment) {
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val durations = listOf("1 hr" to 3600000L, "24 hrs" to 86400000L, "7 days" to 604800000L)
                            durations.forEach { (label, ms) ->
                                val isSelected = uiState.durationMs == ms && uiState.customDurationLabel == null
                                TextButton(
                                    onClick = { viewModel.setDuration(ms, null) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isSelected) SomewhereColors.Background else SomewhereColors.TextSecondary,
                                        containerColor = if (isSelected) SomewhereColors.GlowAccent else androidx.compose.ui.graphics.Color.Transparent
                                    ),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            val isCustomSelected = uiState.customDurationLabel != null
                            TextButton(
                                onClick = {
                                    val calendar = java.util.Calendar.getInstance()
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            calendar.set(year, month, dayOfMonth)
                                            android.app.TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                                    calendar.set(java.util.Calendar.MINUTE, minute)
                                                    val duration = calendar.timeInMillis - System.currentTimeMillis()
                                                    if (duration > 0) {
                                                        val format = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                                        viewModel.setDuration(duration, format.format(calendar.time))
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Time must be in the future", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                                                calendar.get(java.util.Calendar.MINUTE),
                                                true
                                            ).show()
                                        },
                                        calendar.get(java.util.Calendar.YEAR),
                                        calendar.get(java.util.Calendar.MONTH),
                                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        datePicker.minDate = System.currentTimeMillis()
                                    }.show()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (isCustomSelected) SomewhereColors.Background else SomewhereColors.TextSecondary,
                                    containerColor = if (isCustomSelected) SomewhereColors.GlowAccent else androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(uiState.customDurationLabel ?: "Custom", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Categories (made more compact)
                    val categories = listOf("Story", "Memory", "Food", "Music", "Photography", "History", "Hidden Spot", "Event", "Recommendation")
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            val isSelected = uiState.category == category
                            Surface(
                                color = if (isSelected) SomewhereColors.GlowAccent else SomewhereColors.GlassBackground,
                                shape = CircleShape,
                                onClick = { viewModel.setCategory(category) }
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) SomewhereColors.Background else SomewhereColors.TextPrimary
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SomewhereButton(
                            text = "Retake",
                            onClick = { viewModel.reset() }
                        )

                        SomewhereButton(
                            text = if (uiState.isSaving) "Saving..." else "Mark this place",
                            enabled = uiState.text.isNotBlank() && !uiState.isSaving,
                            onClick = {
                                getCurrentLocation(context) { result ->
                                    if (result.isFallback) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Unable to get location")
                                        }
                                        return@getCurrentLocation
                                    }

                                    if (result.accuracyMeters > LocationUtils.ACCURACY_WARNING_METERS) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Low GPS accuracy")
                                        }
                                    }

                                    viewModel.saveDrop(result.latitude, result.longitude)
                                }
                            }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun HoldToRecordButton(
    isRecording: Boolean,
    onPressStart: () -> Boolean,
    onPressEnd: (Long) -> Unit
) {
    var pressStarted by remember { mutableStateOf(false) }
    var pressStartMillis by remember { mutableLongStateOf(0L) }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isRecording) SomewhereColors.AccentSubtle else SomewhereColors.Card)
            .border(0.5.dp, SomewhereColors.CardBorder, RoundedCornerShape(28.dp))
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pressStarted = onPressStart()
                        pressStartMillis = System.currentTimeMillis()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (pressStarted) {
                            val heldMillis = System.currentTimeMillis() - pressStartMillis
                            onPressEnd(heldMillis.coerceAtLeast(0L))
                        }
                        pressStarted = false
                        true
                    }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Hold to record",
            tint = if (isRecording) SomewhereColors.Background else SomewhereColors.TextPrimary
        )
    }
}

@Composable
private fun WaveformBars(samples: List<Float>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        samples.forEach { sample ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(waveHeight(sample))
                    .clip(RoundedCornerShape(2.dp))
                    .background(SomewhereColors.TextPrimary)
            )
        }
    }
}

@Composable
private fun AnimatedWaveform(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "previewWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(12) { index ->
            val dynamic = kotlin.math.abs(kotlin.math.sin((index / 3f + phase) * Math.PI)).toFloat()
            val sample = if (active) (0.2f + dynamic * 0.8f) else 0.16f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(waveHeight(sample))
                    .clip(RoundedCornerShape(2.dp))
                    .background(SomewhereColors.TextPrimary.copy(alpha = if (active) 1f else 0.5f))
            )
        }
    }
}

private fun waveHeight(sample: Float): Dp {
    return (8 + (sample * 28f)).dp
}

/**
 * Captures a photo to internal storage and returns the URI.
 */
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onCaptured: (Uri) -> Unit
) {
    val capture = imageCapture ?: return

    val photoFile = File(
        context.filesDir,
        "drop_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onCaptured(Uri.fromFile(photoFile))
            }

            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun startAudioRecording(context: Context): Pair<MediaRecorder, Uri>? {
    return runCatching {
        val audioFile = File(
            context.filesDir,
            "audiodrop_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a"
        )
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        recorder to Uri.fromFile(audioFile)
    }.getOrNull()
}

private fun stopAudioRecording(recorder: MediaRecorder?, outputUri: Uri?): Uri? {
    if (recorder == null || outputUri == null) return null
    return runCatching {
        recorder.stop()
        recorder.release()
        val path = outputUri.path ?: return null
        val file = File(path)
        if (file.exists()) outputUri else null
    }.getOrElse {
        runCatching { recorder.release() }
        runCatching {
            outputUri.path?.let { path -> File(path).delete() }
        }
        null
    }
}

private fun discardAudioRecording(recorder: MediaRecorder?, outputUri: Uri?) {
    if (recorder == null || outputUri == null) return
    runCatching { recorder.stop() }
    runCatching { recorder.release() }
    runCatching {
        outputUri.path?.let { path -> File(path).delete() }
    }
}

private fun playAudio(
    context: Context,
    uri: Uri?,
    onComplete: () -> Unit
): MediaPlayer? {
    if (uri == null) return null
    return runCatching {
        MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            start()
            setOnCompletionListener { player ->
                onComplete()
                player.release()
            }
        }
    }.getOrNull()
}

/**
 * Gets the last known location. Falls back to 0,0 if unavailable.
 */
private data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val isFallback: Boolean
)

private fun getCurrentLocation(
    context: Context,
    onLocation: (LocationResult) -> Unit
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocation(
                        LocationResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            isFallback = false
                        )
                    )
                } else {
                    onLocation(LocationResult(0.0, 0.0, Float.MAX_VALUE, true))
                }
            }
            .addOnFailureListener {
                onLocation(LocationResult(0.0, 0.0, Float.MAX_VALUE, true))
            }
    } catch (_: SecurityException) {
        onLocation(LocationResult(0.0, 0.0, Float.MAX_VALUE, true))
    }
}
