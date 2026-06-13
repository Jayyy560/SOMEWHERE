package com.somewhere.app.ui.screen

import android.Manifest
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotionEnabled()
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }

    // Text to Speech for Curator
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = java.util.Locale.US
            }
        }
        tts.value = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(uiState.curatedDrop) {
        uiState.curatedDrop?.let { (intro, text) ->
            tts.value?.speak("$intro. $text", TextToSpeech.QUEUE_FLUSH, null, "drop_curator")
            snackbarHostState.showSnackbar(
                message = "Whisper Mode: Playing selected drop",
                duration = SnackbarDuration.Short
            )
        }
    }

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
            val drops = uiState.nearbyDrops
            val zoneIndex = mutableMapOf<String, Int>()

            drops.forEachIndexed { index, item ->
                // Map the angular difference to screen pixels. 
                // A typical phone camera has roughly a 60 degree horizontal FOV (±30 degrees).
                // So an angle of 30 degrees should put the card at the very edge of the screen.
                val pixelsPerDegree = screenWidthPx / 60f
                val horizontalOffsetPx = item.angleDegrees * pixelsPerDegree

                // Group drops by horizontal zone, then stagger vertically within each zone.
                // This prevents cards at similar bearings from overlapping.
                val zone = when {
                    item.isPrimary -> "primary"
                    item.angleDegrees < -5f -> "left"
                    item.angleDegrees > 5f -> "right"
                    else -> "center"
                }
                val slot = zoneIndex[zone] ?: 0
                zoneIndex[zone] = slot + 1

                val verticalOffsetDp = when (index) {
                    0 -> 0.dp
                    1 -> (-90).dp
                    2 -> 90.dp
                    3 -> (-180).dp
                    4 -> 180.dp
                    5 -> (-260).dp
                    6 -> 260.dp
                    else -> {
                        val sign = if (index % 2 == 0) -1 else 1
                        val magnitude = 100 + index * 30
                        (sign * magnitude).dp
                    }
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

            // Empty state — animated radar pulse
            if (uiState.hasLocation && uiState.nearbyDrops.isEmpty()) {
                RadarEmptyState(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Compass heading HUD — translucent pill with direction label
            CompassHud(
                heading = uiState.heading,
                accuracyMeters = uiState.locationAccuracyMeters,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
            )

            // Drop count badge
            if (uiState.hasLocation && uiState.nearbyDrops.isNotEmpty()) {
                val unlockedCount = uiState.nearbyDrops.count { it.isUnlocked }
                val totalCount = uiState.nearbyDrops.size

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // AI Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.summarizePlace() },
                            colors = ButtonDefaults.buttonColors(containerColor = SomewhereColors.GlassBackground),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp), tint = SomewhereColors.GlowAccent)
                            Spacer(Modifier.width(6.dp))
                            Text("Summary", color = SomewhereColors.TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { viewModel.curateDrop() },
                            colors = ButtonDefaults.buttonColors(containerColor = SomewhereColors.GlassBackground),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Hearing, contentDescription = "Whisper", modifier = Modifier.size(16.dp), tint = SomewhereColors.GlowAccent)
                            Spacer(Modifier.width(6.dp))
                            Text("Whisper", color = SomewhereColors.TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SomewhereColors.GlassBackground)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (unlockedCount > 0) {
                                "$unlockedCount unlocked · $totalCount nearby"
                            } else {
                                "$totalCount nearby"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = SomewhereColors.DistancePillText
                        )
                    }
                }
            }

            if (!uiState.hasLocation) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                ) {
                    Text(
                        text = "locating…",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SomewhereColors.TextSecondary
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

            // Place Summary Dialog
            if (uiState.isSummarizing || uiState.summaryText != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearSummary() },
                    containerColor = SomewhereColors.Card,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = SomewhereColors.GlowAccent)
                            Spacer(Modifier.width(8.dp))
                            Text("Place Summary", color = SomewhereColors.TextPrimary)
                        }
                    },
                    text = {
                        if (uiState.isSummarizing) {
                            CircularProgressIndicator(color = SomewhereColors.GlowAccent)
                        } else {
                            Text(uiState.summaryText ?: "", color = SomewhereColors.TextSecondary)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearSummary() }) {
                            Text("Close", color = SomewhereColors.GlowAccent)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Compass heading pill — shows direction (N/NE/E/SE/S/SW/W/NW) and heading degrees.
 * Accuracy is color-coded: green (good), yellow (moderate), red (poor).
 */
@Composable
private fun CompassHud(
    heading: Float,
    accuracyMeters: Float?,
    modifier: Modifier = Modifier
) {
    val directionLabel = headingToDirection(heading)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Heading pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SomewhereColors.GlassBackground)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = directionLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SomewhereColors.GlowAccent
            )
            Text(
                text = "${heading.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = SomewhereColors.TextSecondary
            )
        }

        // Accuracy pill
        accuracyMeters?.let { accuracy ->
            val accuracyColor = when {
                accuracy <= 10f -> SomewhereColors.AccuracyGood
                accuracy <= LocationUtils.ACCURACY_WARNING_METERS -> SomewhereColors.AccuracyModerate
                else -> SomewhereColors.AccuracyPoor
            }
            val accuracyLabel = when {
                accuracy <= 10f -> "±${accuracy.toInt()}m"
                accuracy <= LocationUtils.ACCURACY_WARNING_METERS -> "±${accuracy.toInt()}m"
                else -> "low ±${accuracy.toInt()}m"
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SomewhereColors.GlassBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Color dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accuracyColor)
                )
                Text(
                    text = accuracyLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = SomewhereColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Animated radar-style empty state with pulsing rings and "scanning..." text.
 */
@Composable
private fun RadarEmptyState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val reduceMotion = rememberReduceMotionEnabled()

    // Ring 1
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    // Ring 2 (offset timing)
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000)
        ),
        label = "ring2Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000)
        ),
        label = "ring2Alpha"
    )

    // Ring 3 (more offset)
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(2000)
        ),
        label = "ring3Scale"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceMotion) 0 else 3000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(2000)
        ),
        label = "ring3Alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Pulsing rings
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(ring1Scale)
                    .alpha(ring1Alpha)
                    .background(
                        SomewhereColors.GlowAccent.copy(alpha = 0.3f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(ring2Scale)
                    .alpha(ring2Alpha)
                    .background(
                        SomewhereColors.GlowAccent.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(ring3Scale)
                    .alpha(ring3Alpha)
                    .background(
                        SomewhereColors.TextPrimary.copy(alpha = 0.15f),
                        CircleShape
                    )
            )

            // Center dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(SomewhereColors.GlowAccent, CircleShape)
            )
        }

        Text(
            text = "scanning nearby…",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = SomewhereColors.TextSecondary
        )
    }
}

private fun headingToDirection(heading: Float): String {
    val normalized = ((heading % 360) + 360) % 360
    return when {
        normalized < 22.5f || normalized >= 337.5f -> "N"
        normalized < 67.5f -> "NE"
        normalized < 112.5f -> "E"
        normalized < 157.5f -> "SE"
        normalized < 202.5f -> "S"
        normalized < 247.5f -> "SW"
        normalized < 292.5f -> "W"
        else -> "NW"
    }
}
