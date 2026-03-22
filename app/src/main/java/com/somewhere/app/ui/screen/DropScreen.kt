package com.somewhere.app.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    onComplete: () -> Unit,
    viewModel: DropViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotionEnabled()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    // Navigate back after successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    // ImageCapture use case reference
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

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

    PermissionGate(
        title = "Camera + Location",
        description = "Capture a photo and attach it to this exact place.",
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomewhereColors.Background)
                .alpha(contentAlpha)
        ) {
            // Camera preview
            if (uiState.capturedImageUri == null) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uiState.capturedImageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Captured photo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(SomewhereColors.Background.copy(alpha = 0.92f))
                    .padding(24.dp)
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
                    // Text input
                    BasicTextField(
                        value = uiState.text,
                        onValueChange = viewModel::onTextChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SomewhereColors.Card)
                            .border(0.5.dp, SomewhereColors.CardBorder, RoundedCornerShape(4.dp))
                            .padding(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = SomewhereColors.TextPrimary
                        ),
                        cursorBrush = SolidColor(SomewhereColors.AccentSubtle),
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            Box {
                                if (uiState.text.isEmpty()) {
                                    Text(
                                        text = "What happened here?",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = SomewhereColors.TextMuted
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Character count
                    Text(
                        text = "${uiState.text.length}/120",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )

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
