package com.somewhere.app.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.component.SomewhereTopAppBar
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.PermissionRequestHistory
import com.somewhere.app.util.SettingsUtils
import com.somewhere.app.viewmodel.ImageMatchViewModel
import com.somewhere.app.viewmodel.MatchState
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FindSpotScreen(
    originalImageUrl: String,
    onBack: () -> Unit,
    viewModel: ImageMatchViewModel = hiltViewModel()
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val matchState by viewModel.matchState.collectAsState()
    val score by viewModel.matchScore.collectAsState()
    val context = LocalContext.current
    var hasRequestedCamera by remember {
        mutableStateOf(
            PermissionRequestHistory.wasRequested(
                context,
                android.Manifest.permission.CAMERA
            )
        )
    }
    val cameraPermanentlyDenied =
        !cameraPermissionState.status.isGranted &&
            hasRequestedCamera &&
            (cameraPermissionState.status as? PermissionStatus.Denied)
                ?.shouldShowRationale == false

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            PermissionRequestHistory.markRequested(
                context,
                listOf(android.Manifest.permission.CAMERA)
            )
            hasRequestedCamera = true
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted, originalImageUrl) {
        if (cameraPermissionState.status.isGranted) {
            viewModel.startLiveAnalysis(originalImageUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
    ) {
        if (cameraPermissionState.status.isGranted) {
            when (matchState) {
                MatchState.LOADING_ORIGINAL -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = SomewhereColors.Accent)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading original image...", color = SomewhereColors.TextPrimary)
                    }
                }
                MatchState.ANALYZING -> {
                    CameraLiveView(
                        onFrameAnalyzed = { bitmap ->
                            viewModel.processLiveFrame(bitmap)
                        }
                    )
                    
                    // Live UI Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val currentScore = score ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Point your camera to find the spot",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            // Live score bubble
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(if (currentScore > 60) SomewhereColors.Accent else Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$currentScore%",
                                    color = if (currentScore > 60) Color.Black else Color.White,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
                MatchState.RESULT -> {
                    MatchResultCard(
                        score = score ?: -1,
                        onRetry = { viewModel.startLiveAnalysis(originalImageUrl) },
                        onDone = onBack
                    )
                }
                else -> {}
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera permission is required to find the exact spot.",
                    color = SomewhereColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                SomewhereButton(
                    text = if (cameraPermanentlyDenied) "Open Settings" else "Grant Permission",
                    onClick = {
                        if (cameraPermanentlyDenied) {
                            SettingsUtils.openAppSettings(context)
                        } else {
                            PermissionRequestHistory.markRequested(
                                context,
                                listOf(android.Manifest.permission.CAMERA)
                            )
                            hasRequestedCamera = true
                            cameraPermissionState.launchPermissionRequest()
                        }
                    }
                )
            }
        }

        SomewhereTopAppBar(
            title = "Find Exact Spot",
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .systemBarsPadding()
        )
    }
}

@Composable
fun CameraLiveView(onFrameAnalyzed: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val previewView = remember { PreviewView(context) }
    
    val imageAnalysis = remember {
        androidx.camera.core.ImageAnalysis.Builder()
            .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    
    LaunchedEffect(previewView) {
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            try {
                val bitmap = imageProxy.toBitmap()
                val matrix = Matrix()
                matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                onFrameAnalyzed(rotatedBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
            }
        }

        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MatchResultCard(score: Int, onRetry: () -> Unit, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SomewhereColors.Card)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (score >= 80) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SomewhereColors.Accent,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "$score% Match",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = SomewhereColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Looks like you've found the original viewpoint!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SomewhereColors.TextSecondary
                    )
                } else if (score == -1) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = SomewhereColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Could not load the original image.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SomewhereColors.TextSecondary
                    )
                } else {
                    Text(
                        text = "$score% Match",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = SomewhereColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Try moving closer or changing your angle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SomewhereColors.TextSecondary
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                SomewhereButton(text = "Try Again", onClick = onRetry)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDone) {
                    Text("Done", color = SomewhereColors.TextSecondary)
                }
            }
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, ContextCompat.getMainExecutor(this))
}
