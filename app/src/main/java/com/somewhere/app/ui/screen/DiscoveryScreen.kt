package com.somewhere.app.ui.screen

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.somewhere.app.ui.component.PermissionGate
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.component.DropOverlayCard
import com.somewhere.app.ui.component.PingPulse
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DiscoveryViewModel

/**
 * Discovery screen — fullscreen camera with spatially positioned overlays.
 *
 * Overlay cards shift horizontally based on compass heading vs. drop bearing.
 * Locked items appear blurred with "move closer" text.
 * Unlocked items are tappable and expand to a detail sheet.
 * Ping pulse fires when a drop is first discovered.
 */
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotionEnabled()
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }

    // Entrance fade
    var visible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 500),
        label = "discoveryFade"
    )
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    PermissionGate(
        title = "Camera + Location",
        description = "We use your camera to reveal nearby drops, and location to place them accurately.",
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    ) {
        // Start/stop tracking with lifecycle
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.startTracking()
                    Lifecycle.Event.ON_STOP -> viewModel.stopTracking()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomewhereColors.Background)
                .alpha(contentAlpha)
        ) {
            // Fullscreen camera preview
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

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay cards positioned by compass offset
            uiState.nearbyDrops.forEachIndexed { index, item ->
                // Map the angular difference to screen pixels. 
                // A typical phone camera has roughly a 60 degree horizontal FOV (±30 degrees).
                // So an angle of 30 degrees should put the card at the very edge of the screen.
                val pixelsPerDegree = screenWidthPx / 60f
                val horizontalOffsetPx = item.angleDegrees * pixelsPerDegree

                // Vertical offset: primary gets a slightly higher position
                val verticalOffsetDp = when {
                    item.isPrimary -> 0.dp
                    index % 2 == 0 -> (-60).dp
                    else -> 50.dp
                }
                val verticalOffsetPx = with(density) { verticalOffsetDp.toPx() }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset {
                            IntOffset(
                                x = horizontalOffsetPx.toInt(),
                                y = verticalOffsetPx.toInt()
                            )
                        }
                ) {
                    // Ping pulse for newly discovered items
                    if (item.isNewlyDiscovered) {
                        PingPulse(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    DropOverlayCard(
                        item = item,
                        onTap = {
                            if (item.isUnlocked) {
                                viewModel.selectDrop(item)
                            }
                        }
                    )
                }
            }

            // Empty state — no drops nearby
            if (uiState.hasLocation && uiState.nearbyDrops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                ) {
                    Text(
                        text = "nothing nearby — walk within ${LocationUtils.DISCOVERY_RADIUS.toInt()}m",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Compass heading + accuracy indicator
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${uiState.heading.toInt()}°",
                    style = MaterialTheme.typography.labelSmall
                )
                uiState.locationAccuracyMeters?.let { accuracy ->
                    val accuracyText = if (accuracy <= LocationUtils.ACCURACY_WARNING_METERS) {
                        "accuracy ±${accuracy.toInt()}m"
                    } else {
                        "low accuracy ±${accuracy.toInt()}m"
                    }
                    Text(
                        text = accuracyText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (!uiState.hasLocation) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp)
                ) {
                    Text(
                        text = "locating...",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Detail overlay — shown when a drop is selected
            uiState.selectedDrop?.let { selected ->
                DropDetailSheet(
                    item = selected,
                    onDismiss = { viewModel.selectDrop(null) },
                    onDelete = { viewModel.deleteDrop(selected) },
                    onReport = { viewModel.reportDrop(selected) }
                )
            }
        }
    }
}
