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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.somewhere.app.ui.component.PermissionGate
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.StableLocationFix
import com.somewhere.app.util.PlacementPin
import com.somewhere.app.util.PlacementSelection
import com.somewhere.app.util.collectStableLocation
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DropViewModel
import com.somewhere.app.viewmodel.DropCreationStep
import com.somewhere.app.util.NetworkStatus
import com.somewhere.app.util.rememberNetworkStatus
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private const val MAX_AUDIO_RECORDING_MS = 20_000L
private const val MIN_AUDIO_RECORDING_MS = 600L

private val placementSelectionSaver = mapSaver(
    save = { selection: PlacementSelection ->
        mapOf(
            "gpsLat" to selection.gpsFix?.latitude,
            "gpsLon" to selection.gpsFix?.longitude,
            "gpsAccuracy" to selection.gpsFix?.accuracyMeters,
            "gpsAt" to selection.gpsFix?.capturedAtMillis,
            "gpsProvider" to selection.gpsFix?.provider,
            "pinLat" to selection.pin?.latitude,
            "pinLon" to selection.pin?.longitude,
            "pinAt" to selection.pin?.selectedAtMillis
        )
    },
    restore = { saved ->
        val gps = (saved["gpsLat"] as? Double)?.let { latitude ->
            StableLocationFix(
                latitude, saved["gpsLon"] as Double, saved["gpsAccuracy"] as Float,
                saved["gpsAt"] as Long, saved["gpsProvider"] as? String
            )
        }
        val pin = (saved["pinLat"] as? Double)?.let { latitude ->
            PlacementPin(latitude, saved["pinLon"] as Double, saved["pinAt"] as Long)
        }
        PlacementSelection(gps, pin)
    }
)

/**
 * Drop screen — capture a photo, write a message, mark this place.
 *
 * Flow:
 * 1. Fullscreen camera preview
 * 2. Capture button → freezes image
 * 3. Text input appears
 * 4. "Mark this place" → saves drop with current GPS
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DropScreen(
    imageCapture: ImageCapture?,
    triggerCapture: Boolean,
    onCaptureTriggered: () -> Unit,
    onFocusTap: (Float, Float) -> Unit,
    onComplete: () -> Unit,
    cameraError: String? = null,
    onRetryCamera: () -> Unit = {},
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
    var showDiscardDialog by remember { mutableStateOf(false) }
    var placementSelection by rememberSaveable(
        uiState.capturedImageUri?.toString(),
        stateSaver = placementSelectionSaver
    ) { mutableStateOf(PlacementSelection()) }
    val locationFix = placementSelection.gpsFix
    var locationProgress by remember(uiState.capturedImageUri) { mutableStateOf<StableLocationFix?>(null) }
    var isLocating by remember(uiState.capturedImageUri) { mutableStateOf(false) }
    var locationTimedOut by remember(uiState.capturedImageUri) { mutableStateOf(false) }
    var acceptLowAccuracy by remember(uiState.capturedImageUri) { mutableStateOf(false) }
    var locationRetry by remember(uiState.capturedImageUri) { mutableIntStateOf(0) }
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val microphonePermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val networkStatus = rememberNetworkStatus()
    var showVisualPlacement by remember { mutableStateOf(false) }
    if (showVisualPlacement && locationFix != null) {
        VisualPlacementScreen(
            gpsFix = locationFix,
            onPlaced = { placement ->
                placementSelection = placementSelection.copy(pin = null)
                viewModel.setVisualPlacement(placement)
                showVisualPlacement = false
            },
            onClose = { showVisualPlacement = false }
        )
    }

    suspend fun acquirePlacementLocation(maxAccuracyMeters: Float = 100f) {
        isLocating = true
        locationTimedOut = false
        locationProgress = null
        acceptLowAccuracy = false
        try {
            val result = collectStableLocation(context, maxAccuracyMeters) { currentBest ->
                locationProgress = currentBest
            }
            placementSelection = placementSelection.copy(gpsFix = result)
            locationTimedOut = result == null
        } finally {
            isLocating = false
        }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Get file name and size safely
                var name = "attachment"
                var size = 0L
                var type = context.contentResolver.getType(it) ?: "application/octet-stream"
                
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex) ?: "attachment"
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
                
                if (size > 50 * 1024 * 1024) {
                    scope.launch { snackbarHostState.showSnackbar("File too large. Max 50MB.") }
                } else {
                    viewModel.onFileAttached(it, name, size, type)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch { snackbarHostState.showSnackbar("Error reading file") }
            }
        }
    }
    
    LaunchedEffect(uiState.capturedImageUri) {
        onCreationStateChanged(uiState.capturedImageUri != null)
    }
    androidx.activity.compose.BackHandler(enabled = uiState.capturedImageUri != null) {
        showDiscardDialog = true
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

    // Navigate back after successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            if (!com.somewhere.app.PrototypeFeatures.SHOW_HITCHHIKER || !uiState.isHitchhiker) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                viewModel.reset()
                onComplete()
            } else {
                // The animation will play, and it will handle calling onComplete()
            }
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
    var isUIHidden by remember { mutableStateOf(false) }
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

    LaunchedEffect(
        uiState.creationStep,
        uiState.capturedImageUri,
        locationPermission.status.isGranted,
        locationRetry
    ) {
        if (
            uiState.capturedImageUri != null &&
            uiState.creationStep == DropCreationStep.LOCATION &&
            locationPermission.status.isGranted &&
            placementSelection.gpsFix == null
        ) {
            acquirePlacementLocation()
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
        title = "Camera Access",
        description = "Use the camera to capture the place where you want to leave a Drop.",
        permissions = listOf(
            android.Manifest.permission.CAMERA
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                // Do not apply background color so the camera shines through
                .alpha(contentAlpha)
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
                            color = com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                ) {
                    // Small center dot
                    Box(modifier = Modifier.size(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor).align(Alignment.Center))
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

            // This layer sits above the photo/camera but below the creation panel.
            // A short tap focuses the live camera. Only a deliberate hold hides
            // the UI over a captured photo, and panel interactions never trigger it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.capturedImageUri) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val releasedBeforeLongPress = withTimeoutOrNull(
                                viewConfiguration.longPressTimeoutMillis
                            ) {
                                waitForUpOrCancellation()
                            }
                            if (releasedBeforeLongPress == null) {
                                isUIHidden = true
                                try {
                                    waitForUpOrCancellation()
                                } finally {
                                    isUIHidden = false
                                }
                            } else if (uiState.capturedImageUri == null) {
                                focusPoint = down.position
                                focusTrigger++
                                onFocusTap(
                                    down.position.x / size.width.toFloat(),
                                    down.position.y / size.height.toFloat()
                                )
                            }
                        }
                    }
            )

            // Bottom panel
            androidx.compose.animation.AnimatedVisibility(
                visible = !isUIHidden,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val hasCapturedPhoto = uiState.capturedImageUri != null
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(SomewhereColors.Background.copy(alpha = 0.92f))
                        .navigationBarsPadding()
                        .padding(bottom = 104.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.capturedImageUri == null) {
                    when {
                        cameraError != null -> {
                            Text(
                                cameraError,
                                color = SomewhereColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            SomewhereButton(text = "Retry Camera", onClick = onRetryCamera)
                        }
                        imageCapture == null -> {
                            CircularProgressIndicator(color = SomewhereColors.Accent)
                            Text("Starting camera…", color = SomewhereColors.TextSecondary)
                        }
                        else -> SomewhereButton(
                            text = "Capture",
                            onClick = {
                                takePhoto(context, imageCapture) { uri ->
                                    viewModel.onPhotoCaptured(uri)
                                }
                            }
                        )
                    }
                } else {
                    DropProgressHeader(step = uiState.creationStep)

                    if (uiState.creationStep == DropCreationStep.MESSAGE) {
                    // Top Row: Text Input & Audio Record Button side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BasicTextField(
                            value = uiState.text,
                            onValueChange = { 
                                if (it.length <= 120) {
                                    viewModel.onTextChanged(it)
                                }
                            },
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
                                if (!microphonePermission.status.isGranted) {
                                    microphonePermission.launchPermissionRequest()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Allow microphone access, then hold again to record"
                                        )
                                    }
                                    return@HoldToRecordButton false
                                }
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
                                color = com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor
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

                    OutlinedButton(
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            SomewhereColors.CardBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = SomewhereColors.TextSecondary
                        )
                    ) {
                        Text(
                            text = "Retake photo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    }

                    if (uiState.creationStep == DropCreationStep.ENHANCEMENTS) {
                    // Options Row: Type & Ghost Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(2.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (!uiState.isMoment) 1.dp else 0.5.dp,
                                    color = if (!uiState.isMoment) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.CardBorder
                                    }
                                ),
                                onClick = { viewModel.setDropType(false) }
                            ) {
                                Text(
                                    "Permanent",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!uiState.isMoment) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.TextSecondary
                                    }
                                )
                            }
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(2.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (uiState.isMoment) 1.dp else 0.5.dp,
                                    color = if (uiState.isMoment) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.CardBorder
                                    }
                                ),
                                onClick = { viewModel.setDropType(true) }
                            ) {
                                Text(
                                    "Moment",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isMoment) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.TextSecondary
                                    }
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Ghost",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isAnonymous) {
                                    SomewhereColors.TextPrimary
                                } else {
                                    SomewhereColors.TextMuted
                                }
                            )
                            Switch(
                                checked = uiState.isAnonymous,
                                onCheckedChange = { viewModel.setAnonymous(it) },
                                modifier = Modifier.scale(0.7f).padding(start = 4.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SomewhereColors.Background,
                                    checkedTrackColor = SomewhereColors.TextPrimary
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
                                        contentColor = if (isSelected) {
                                            SomewhereColors.TextPrimary
                                        } else {
                                            SomewhereColors.TextSecondary
                                        },
                                        containerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.5.dp,
                                            color = if (isSelected) {
                                                SomewhereColors.TextPrimary
                                            } else {
                                                SomewhereColors.CardBorder
                                            },
                                            shape = RoundedCornerShape(2.dp)
                                        ),
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
                                    contentColor = if (isCustomSelected) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.TextSecondary
                                    },
                                    containerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .border(
                                        width = if (isCustomSelected) 1.dp else 0.5.dp,
                                        color = if (isCustomSelected) {
                                            SomewhereColors.TextPrimary
                                        } else {
                                            SomewhereColors.CardBorder
                                        },
                                        shape = RoundedCornerShape(2.dp)
                                    ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(uiState.customDurationLabel ?: "Custom", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Categories (made more compact)
                    val categories = com.somewhere.app.util.CategoryUtils.CATEGORIES
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            val isSelected = uiState.category == category
                            Surface(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(2.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.CardBorder
                                    }
                                ),
                                onClick = { viewModel.setCategory(category) }
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        SomewhereColors.TextPrimary
                                    } else {
                                        SomewhereColors.TextSecondary
                                    }
                                )
                            }
                        }
                    }

                    // Dead Drop Attachment Preview
                    if (uiState.deadDropFileUri != null) {
                        Surface(
                            color = SomewhereColors.GlassBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.AttachFile,
                                    contentDescription = "Attachment",
                                    tint = SomewhereColors.TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.deadDropFileName ?: "Attachment",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SomewhereColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    val sizeMb = (uiState.deadDropFileSize ?: 0L) / (1024f * 1024f)
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.2f MB", sizeMb),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SomewhereColors.TextSecondary
                                    )
                                }
                                IconButton(onClick = { viewModel.clearFile() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = SomewhereColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.deadDropFileUri == null) {
                        SomewhereButton(
                            text = "Attach File",
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (com.somewhere.app.PrototypeFeatures.SHOW_HITCHHIKER) {
                        HitchhikerToggleBox(
                            isHitchhiker = uiState.isHitchhiker,
                            onToggle = { viewModel.setHitchhiker(it) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Release as a Hitchhiker",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SomewhereColors.TextPrimary
                                    )
                                    Text(
                                        text = "Anyone can carry this Drop to new locations. Its journey will be recorded forever.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SomewhereColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "This Drop could travel for years and be carried by hundreds of people.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = SomewhereColors.TextSecondary
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = uiState.isHitchhiker,
                                    onCheckedChange = { viewModel.setHitchhiker(it) },
                                    modifier = Modifier.scale(0.7f),
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = SomewhereColors.TextPrimary
                                    )
                                )
                            }
                        }
                    }
                    }

                    if (uiState.creationStep == DropCreationStep.LOCATION) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                            Text(
                                if (uiState.visualPlacement != null) "Visual spot saved — visitors can recognize this exact place."
                                else "Pin the Drop to a real surface, including indoors.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        if (locationFix == null || System.currentTimeMillis() - locationFix.capturedAtMillis > 120_000L) {
                                            acquirePlacementLocation(maxAccuracyMeters = 500f)
                                        }
                                        if (placementSelection.gpsFix != null) showVisualPlacement = true
                                        else snackbarHostState.showSnackbar("A rough location is still needed to make this Drop discoverable. Enable location services and retry.")
                                    }
                                },
                                enabled = locationPermission.status.isGranted && !isLocating && !uiState.isSaving &&
                                    networkStatus != NetworkStatus.UNAVAILABLE,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (uiState.visualPlacement != null) "Rescan visual spot" else "Scan and place in AR")
                            }
                            if (uiState.visualPlacement != null) {
                                TextButton(onClick = { viewModel.setVisualPlacement(null) }) {
                                    Text("Remove visual placement")
                                }
                                Text("Changing the map pin removes the visual placement.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        LocationConfirmationContent(
                            locationFix = if (isLocating) locationProgress else uiState.visualPlacement?.let {
                                StableLocationFix(it.latitude, it.longitude, it.accuracyMeters, it.capturedAtMillis, it.provider)
                            } ?: locationFix,
                            selectedPin = placementSelection.pin,
                            canChangePin = !isLocating && !uiState.isSaving,
                            hasVisualPlacement = uiState.visualPlacement != null,
                            isLocating = isLocating,
                            timedOut = locationTimedOut,
                            isOffline = networkStatus == NetworkStatus.UNAVAILABLE,
                            hasLocationPermission = locationPermission.status.isGranted,
                            lowAccuracyAccepted = acceptLowAccuracy,
                            onChooseMapLocation = { latitude, longitude ->
                                viewModel.setVisualPlacement(null)
                                placementSelection = placementSelection.withPin(latitude, longitude, System.currentTimeMillis())
                            },
                            onUseGpsLocation = {
                                viewModel.setVisualPlacement(null)
                                placementSelection = placementSelection.copy(pin = null)
                            },
                            onRequestPermission = locationPermission::launchPermissionRequest,
                            onAcceptLowAccuracy = { acceptLowAccuracy = true },
                            onRetry = {
                                placementSelection = PlacementSelection()
                                locationRetry++
                            }
                        )
                    }

            }
                    if (hasCapturedPhoto) {
                        DropStepActions(
                            step = uiState.creationStep,
                            hasMessage = uiState.text.isNotBlank(),
                            isSaving = uiState.isSaving,
                            isOffline = networkStatus == NetworkStatus.UNAVAILABLE,
                            hasLocation = uiState.visualPlacement != null || (!isLocating && locationFix != null && (
                                placementSelection.pin != null ||
                                    locationFix.accuracyMeters <= LocationUtils.ACCURACY_WARNING_METERS ||
                                    acceptLowAccuracy
                                )),
                            isHitchhiker = com.somewhere.app.PrototypeFeatures.SHOW_HITCHHIKER && uiState.isHitchhiker,
                            onBack = {
                                viewModel.setCreationStep(
                                    when (uiState.creationStep) {
                                        DropCreationStep.MESSAGE -> DropCreationStep.MESSAGE
                                        DropCreationStep.ENHANCEMENTS -> DropCreationStep.MESSAGE
                                        DropCreationStep.LOCATION -> DropCreationStep.ENHANCEMENTS
                                    }
                                )
                            },
                            onContinue = {
                                viewModel.setCreationStep(
                                    when (uiState.creationStep) {
                                        DropCreationStep.MESSAGE -> DropCreationStep.ENHANCEMENTS
                                        DropCreationStep.ENHANCEMENTS -> DropCreationStep.LOCATION
                                        DropCreationStep.LOCATION -> DropCreationStep.LOCATION
                                    }
                                )
                            },
                            onSubmit = {
                                val visual = uiState.visualPlacement
                                if (visual != null) {
                                    viewModel.saveDrop(visual.latitude, visual.longitude, visual.accuracyMeters,
                                        visual.capturedAtMillis, visual.provider)
                                } else placementSelection.location?.let {
                                    viewModel.saveDrop(
                                        latitude = it.latitude,
                                        longitude = it.longitude,
                                        placementAccuracyMeters = it.accuracyMeters,
                                        placementLocationTimestamp = it.capturedAtMillis,
                                        placementLocationProvider = it.provider
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                        )
                    }
                }
        }



            }
            
            if (
                com.somewhere.app.PrototypeFeatures.SHOW_HITCHHIKER &&
                uiState.isSaved &&
                uiState.isHitchhiker
            ) {
                ReleaseAnimationOverlay(
                    onComplete = {
                        viewModel.reset()
                        onComplete()
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    title = { Text("Discard captured photo?") },
                    text = {
                        Text("Your photo, message, and optional enhancements will be removed.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDiscardDialog = false
                                viewModel.reset()
                            }
                        ) {
                            Text("Discard", color = SomewhereColors.Error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Keep draft", color = SomewhereColors.TextPrimary)
                        }
                    },
                    containerColor = SomewhereColors.Surface
                )
            }
        }
    }
}

@Composable
private fun DropProgressHeader(step: DropCreationStep) {
    val steps = listOf(
        DropCreationStep.MESSAGE to "Message",
        DropCreationStep.ENHANCEMENTS to "Enhance",
        DropCreationStep.LOCATION to "Place"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Create Drop",
            style = MaterialTheme.typography.titleLarge,
            color = SomewhereColors.TextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, (itemStep, label) ->
                val isCurrent = itemStep == step
                val isComplete = itemStep.ordinal < step.ordinal
                val labelColor = when {
                    isCurrent -> SomewhereColors.TextPrimary
                    isComplete -> SomewhereColors.TextSecondary
                    else -> SomewhereColors.TextMuted
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "0${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) {
                                com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor
                            } else {
                                labelColor
                            }
                        )
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCurrent) 2.dp else 1.dp)
                            .background(
                                when {
                                    isCurrent -> com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor
                                    isComplete -> SomewhereColors.TextMuted
                                    else -> SomewhereColors.CardBorder
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationConfirmationContent(
    locationFix: StableLocationFix?,
    selectedPin: PlacementPin?,
    canChangePin: Boolean,
    hasVisualPlacement: Boolean,
    isLocating: Boolean,
    timedOut: Boolean,
    isOffline: Boolean,
    hasLocationPermission: Boolean,
    lowAccuracyAccepted: Boolean,
    onChooseMapLocation: (Double, Double) -> Unit,
    onUseGpsLocation: () -> Unit,
    onRequestPermission: () -> Unit,
    onAcceptLowAccuracy: () -> Unit,
    onRetry: () -> Unit
) {
    val accuracy = locationFix?.accuracyMeters
    val cameraPositionState = rememberCameraPositionState()
    val point = selectedPin?.let { LatLng(it.latitude, it.longitude) }
        ?: locationFix?.let { LatLng(it.latitude, it.longitude) }
    val markerState = remember { MarkerState(position = point ?: LatLng(0.0, 0.0)) }
    var hasCenteredMap by remember { mutableStateOf(false) }
    LaunchedEffect(point) {
        point?.let { markerState.position = it }
    }
    LaunchedEffect(locationFix?.capturedAtMillis, selectedPin == null) {
        locationFix?.let { fix ->
            if (!hasCenteredMap || selectedPin == null) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(point ?: LatLng(fix.latitude, fix.longitude), 19f)
                hasCenteredMap = true
            }
        }
    }
    val accuracyColor = when {
        selectedPin != null -> SomewhereColors.Accent
        accuracy == null -> SomewhereColors.TextMuted
        accuracy <= 10f -> SomewhereColors.AccuracyGood
        accuracy <= LocationUtils.ACCURACY_WARNING_METERS -> SomewhereColors.AccuracyModerate
        else -> SomewhereColors.AccuracyPoor
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SomewhereColors.Card,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Confirm this place",
                style = MaterialTheme.typography.titleMedium,
                color = SomewhereColors.TextPrimary
            )
            if (locationFix != null && hasLocationPermission && !isLocating) {
                Text(
                    if (hasVisualPlacement) "The map shows the search area. Your scanned surface is saved in the visual anchor. Tap the map to replace that placement."
                    else "Tap the satellite map to move the pin onto the entrance or landmark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SomewhereColors.TextSecondary
                )
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.HYBRID),
                    onMapClick = { mapPoint ->
                        if (canChangePin) onChooseMapLocation(mapPoint.latitude, mapPoint.longitude)
                    }
                ) {
                    Marker(
                        state = markerState,
                        title = "Drop location",
                        snippet = "Tap elsewhere on the map to move this pin"
                    )
                }
            }
            when {
                isOffline -> {
                    Text(
                        "You are offline. Reconnect before publishing this Drop.",
                        color = SomewhereColors.TextSecondary
                    )
                }
                !hasLocationPermission -> {
                    Text(
                        "Location access is needed now to attach this Drop to the place where you are standing.",
                        color = SomewhereColors.TextSecondary
                    )
                    SomewhereButton(text = "Allow Location", onClick = onRequestPermission)
                }
                isLocating -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = SomewhereColors.Accent
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Stabilizing this place…", color = SomewhereColors.TextSecondary)
                        locationFix?.let { fix ->
                            Text(
                                "Best fix so far: ±${fix.accuracyMeters.toInt()} m",
                                style = MaterialTheme.typography.labelSmall,
                                color = accuracyColor
                            )
                        }
                    }
                }
                locationFix != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accuracyColor)
                        )
                        Text(
                            text = if (hasVisualPlacement) {
                                "Visual placement saved"
                            } else if (selectedPin != null) {
                                "Map pin selected"
                            } else {
                                "Location accurate to about ${locationFix.accuracyMeters.toInt()} m"
                            },
                            color = SomewhereColors.TextPrimary
                        )
                    }
                    Text(
                        text = if (hasVisualPlacement) {
                            "Publish to share this visual spot. The geographic coordinate is used to find nearby Drops, not to choose the indoor floor."
                        } else if (selectedPin != null) {
                            "Check that the marker sits on the real entrance or spot before publishing."
                        } else if (locationFix.accuracyMeters <= LocationUtils.ACCURACY_WARNING_METERS) {
                            "This location is ready to publish."
                        } else if (lowAccuracyAccepted) {
                            "You chose to publish with this approximate location."
                        } else {
                            "Accuracy is low. Retry in an open area, or explicitly use this approximate location."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SomewhereColors.TextSecondary
                    )
                    if (selectedPin != null) {
                        TextButton(onClick = onUseGpsLocation, enabled = canChangePin) {
                            Text("Use GPS point")
                        }
                    } else if (!hasVisualPlacement && locationFix.accuracyMeters > LocationUtils.ACCURACY_WARNING_METERS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SomewhereButton(
                                text = "Retry",
                                onClick = onRetry,
                                minWidth = 0.dp
                            )
                            if (!lowAccuracyAccepted) {
                                SomewhereButton(
                                    text = "Use Anyway",
                                    onClick = onAcceptLowAccuracy,
                                    minWidth = 0.dp
                                )
                            }
                        }
                    }
                }
                timedOut -> {
                    Text(
                        "Location timed out. Check that precise location is enabled and try again.",
                        color = SomewhereColors.TextSecondary
                    )
                    SomewhereButton(text = "Retry Location", onClick = onRetry)
                }
            }
        }
    }
}

@Composable
private fun DropStepActions(
    step: DropCreationStep,
    hasMessage: Boolean,
    isSaving: Boolean,
    isOffline: Boolean,
    hasLocation: Boolean,
    isHitchhiker: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step != DropCreationStep.MESSAGE) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = SomewhereColors.TextSecondary
                )
            ) {
                Text(
                    "Back",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        val enabled = when (step) {
            DropCreationStep.MESSAGE -> hasMessage
            DropCreationStep.ENHANCEMENTS -> true
            DropCreationStep.LOCATION -> hasLocation && !isOffline && !isSaving
        }
        OutlinedButton(
            onClick = if (step == DropCreationStep.LOCATION) onSubmit else onContinue,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(2.dp),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (enabled) SomewhereColors.TextSecondary else SomewhereColors.CardBorder
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = SomewhereColors.TextPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = SomewhereColors.TextMuted
            )
        ) {
            Text(
                text = when (step) {
                    DropCreationStep.MESSAGE -> "Continue"
                    DropCreationStep.ENHANCEMENTS -> "Review Location"
                    DropCreationStep.LOCATION -> when {
                        isSaving -> "Publishing..."
                        isHitchhiker -> "Begin Its Journey"
                        else -> "Mark This Place"
                    }
                }.uppercase(),
                style = MaterialTheme.typography.labelLarge
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

@Composable
fun HitchhikerToggleBox(
    isHitchhiker: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Switch) { onToggle(!isHitchhiker) }
            .border(
                width = if (isHitchhiker) 1.dp else 0.5.dp,
                color = if (isHitchhiker) {
                    SomewhereColors.TextPrimary
                } else {
                    SomewhereColors.CardBorder
                },
                shape = RoundedCornerShape(2.dp)
            )
            .padding(12.dp)
    ) {
        content()
    }
}

@Composable
fun ReleaseAnimationOverlay(onComplete: () -> Unit) {
    val pinY = remember { Animatable(0f) }
    val pinScale = remember { Animatable(1f) }
    val textAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Phase 1: Wait a brief moment
        delay(300)
        
        // Phase 2: Launch pin
        launch {
            pinY.animateTo(
                targetValue = -1500f,
                animationSpec = tween(1500, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f))
            )
        }
        launch {
            pinScale.animateTo(
                targetValue = 0f,
                animationSpec = tween(1500, easing = LinearEasing)
            )
        }
        
        delay(1500)
        
        // Phase 3: Text fade in
        textAlpha.animateTo(1f, tween(500))
        delay(1500)
        
        // Phase 4: Complete
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        // Trail canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            if (pinY.value < 0f) {
                drawLine(
                    color = SomewhereColors.AccentPurple,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f + pinY.value),
                    strokeWidth = 10f * pinScale.value,
                    alpha = pinScale.value,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        
        // Pin
        if (pinScale.value > 0f) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Drop Released Pin",
                tint = SomewhereColors.AccentPurple,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        translationY = pinY.value
                        scaleX = pinScale.value
                        scaleY = pinScale.value
                    }
            )
        }
        
        // Text
        if (textAlpha.value > 0f) {
            Text(
                text = "Hitchhiker Released.",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
    }
}
}
