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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import io.github.sceneview.ar.ARSceneView
import dev.romainguy.kotlin.math.Float3
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.somewhere.app.ui.component.PermissionGate
import com.somewhere.app.ui.component.SpatialDataConsent
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.component.EdgeIndicator
import com.somewhere.app.ui.component.shimmerEffect
import com.somewhere.app.ui.component.DropOverlayCard
import com.somewhere.app.ui.component.PingPulse
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.ui.theme.AppTheme
import com.somewhere.app.ui.theme.LocalAppTheme
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.DropVisuals
import com.somewhere.app.util.AnchorTarget
import com.somewhere.app.util.ARUtils
import com.somewhere.app.util.GeospatialAnchors
import com.somewhere.app.util.GeospatialTrackingGate
import com.somewhere.app.util.SpatialFrame
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.util.NetworkStatus
import com.somewhere.app.util.rememberNetworkStatus
import com.somewhere.app.viewmodel.DiscoveryViewModel

/**
 * Discovery screen — fullscreen camera with spatially positioned overlays.
 *
 * Overlay cards shift horizontally based on compass heading vs. drop bearing.
 * Locked items appear blurred with "move closer" text.
 * Unlocked items are tappable and expand to a detail sheet.
 * Ping pulse fires when a drop is first discovered.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DiscoveryScreen(
    onBack: () -> Unit,
    onFindSpot: (String) -> Unit = {},
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = rememberReduceMotionEnabled()
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }
    val ambient = com.somewhere.app.ui.theme.LocalAmbientColors.current
    val isFoldTheme = LocalAppTheme.current == AppTheme.FOLD
    val networkStatus = rememberNetworkStatus()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var openCameraAfterPermission by remember { mutableStateOf(false) }

    val hiddenDropIds = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val visibleDrops by remember(uiState.nearbyDrops, hiddenDropIds) {
        derivedStateOf { uiState.nearbyDrops.filter { it.drop.id !in hiddenDropIds } }
    }
    
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

    val scope = rememberCoroutineScope()

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
        title = "Location",
        description = "Wayfinder needs your location to calculate which Drops are nearby. The camera view is optional.",
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    ) {
        // Start/stop tracking with lifecycle
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.startTracking()
                    Lifecycle.Event.ON_STOP -> {
                        viewModel.stopTracking()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { 
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.stopTracking()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomewhereColors.Background)
                .alpha(contentAlpha)
        ) {
            val blurRadius by animateDpAsState(
                targetValue = if (uiState.selectedDrop != null) 16.dp else 0.dp,
                label = "glassBlur"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            ) {
                var showListView by rememberSaveable {
                    mutableStateOf(!cameraPermission.status.isGranted)
                }

                LaunchedEffect(cameraPermission.status.isGranted) {
                    if (cameraPermission.status.isGranted && openCameraAfterPermission) {
                        showListView = false
                        openCameraAfterPermission = false
                    }
                }

            // Fullscreen camera preview
            var arCoreSupported by remember { mutableStateOf<Boolean?>(null) }
            var arAvailabilityCheck by remember { mutableIntStateOf(0) }
            var fallbackCameraError by remember { mutableStateOf<String?>(null) }
            var fallbackCameraRetryKey by remember { mutableIntStateOf(0) }

            DisposableEffect(lifecycleOwner) {
                val arResumeObserver = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        arAvailabilityCheck++
                    }
                }
                lifecycleOwner.lifecycle.addObserver(arResumeObserver)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(arResumeObserver)
                }
            }

            LaunchedEffect(arAvailabilityCheck, cameraPermission.status.isGranted) {
                if (!cameraPermission.status.isGranted) {
                    arCoreSupported = false
                    return@LaunchedEffect
                }
                try {
                    val apk = com.google.ar.core.ArCoreApk.getInstance()
                    // Poll until the availability result is no longer transient (cold start can take >1s).
                    var availability = apk.checkAvailability(context)
                    var tries = 0
                    while (availability.isTransient && tries < 15) {
                        kotlinx.coroutines.delay(200)
                        availability = apk.checkAvailability(context)
                        tries++
                    }
                    when (availability) {
                        com.google.ar.core.ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                            // Check Geospatial support on the actual camera session. Opening
                            // a second session here can disrupt the view that is resuming.
                            arCoreSupported = true
                        }
                        com.google.ar.core.ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
                        com.google.ar.core.ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                            // Discovery remains usable through CameraX. Do not unexpectedly
                            // leave the app to launch the ARCore installation flow.
                            arCoreSupported = false
                        }
                        else -> {
                            // Unsupported, unknown, or still transient: use the camera fallback.
                            arCoreSupported = false
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w(
                        "DiscoveryScreen",
                        "ARCore availability check failed; using CameraX",
                        e
                    )
                    arCoreSupported = false
                }
            }

            var arFrame by remember { mutableStateOf<SpatialFrame?>(null) }
            var positioningMessage by remember { mutableStateOf<String?>(null) }
            
            val configuration = LocalConfiguration.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.toInt()
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }.toInt()

            if (!showListView) {
                if (arCoreSupported == true) {
                    val anchorManager = remember { GeospatialAnchors() }
                    val trackingGate = remember { GeospatialTrackingGate() }
                    DisposableEffect(lifecycleOwner, anchorManager) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_PAUSE) {
                                anchorManager.clear()
                                trackingGate.reset()
                                arFrame = null
                                viewModel.onArTrackingLost()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            anchorManager.clear()
                            arFrame = null
                            viewModel.onArTrackingLost()
                        }
                    }
                    SpatialDataConsent(onDecline = { arCoreSupported = false }, content = {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { released ->
                            (released as? ARSceneView)?.let { scene ->
                                scene.onSessionUpdated = null
                                anchorManager.clear()
                                // Release the camera before CameraX starts its fallback preview.
                                runCatching { scene.session?.pause() }
                                scene.destroy()
                            }
                        },
                        factory = { ctx ->
                            var lastFrameUpdate = 0L
                            var lastFrameAt = 0L
                            var lastLocalizedAt = android.os.SystemClock.elapsedRealtime()
                            var lastResolvedAt = lastLocalizedAt
                            try {
                                ARSceneView(ctx).apply {
                                    planeRenderer.isVisible = false
                                    onSessionFailed = { exception ->
                                        android.util.Log.w(
                                            "DiscoveryScreen",
                                            "AR session failed; switching to CameraX",
                                            exception
                                        )
                                        arFrame = null
                                        viewModel.onArTrackingLost()
                                        arCoreSupported = false
                                    }
                                    configureSession { session, config ->
                                        config.cloudAnchorMode = com.google.ar.core.Config.CloudAnchorMode.ENABLED
                                        if (
                                            session.isGeospatialModeSupported(
                                                com.google.ar.core.Config.GeospatialMode.ENABLED
                                            )
                                        ) {
                                            config.geospatialMode =
                                                com.google.ar.core.Config.GeospatialMode.ENABLED
                                        } else {
                                            config.geospatialMode =
                                                com.google.ar.core.Config.GeospatialMode.DISABLED
                                        }
                                    }
                                    onSessionUpdated = { session, frame ->
                                        val now = android.os.SystemClock.elapsedRealtime()
                                        if (lastFrameAt == 0L || now - lastFrameAt > 2_000L) {
                                            trackingGate.reset()
                                            lastLocalizedAt = now
                                            lastResolvedAt = now
                                        }
                                        lastFrameAt = now
                                        val earth = session.earth?.takeIf {
                                            it.earthState == com.google.ar.core.Earth.EarthState.ENABLED
                                        }
                                        val cameraTracking = frame.camera.trackingState == com.google.ar.core.TrackingState.TRACKING
                                        if (cameraTracking) {
                                            lastLocalizedAt = now
                                            val tracking = earth?.trackingState == com.google.ar.core.TrackingState.TRACKING
                                            val pose = if (tracking) earth?.cameraGeospatialPose else null
                                            val localized = trackingGate.update(
                                                tracking = tracking,
                                                horizontalAccuracyMeters = pose?.horizontalAccuracy ?: Double.POSITIVE_INFINITY,
                                                yawAccuracyDegrees = pose?.orientationYawAccuracy ?: Double.POSITIVE_INFINITY,
                                                nowMillis = now
                                            )
                                            if (localized && earth != null && pose != null) {
                                                viewModel.updateArCorePose(
                                                    latitude = pose.latitude,
                                                    longitude = pose.longitude,
                                                    accuracyMeters = pose.horizontalAccuracy.toFloat(),
                                                    heading = ARUtils.headingFromQuaternion(pose.eastUpSouthQuaternion)
                                                )
                                            } else viewModel.onArTrackingLost()
                                            val targets = viewModel.uiState.value.nearbyDrops
                                                .filter { it.drop.id !in hiddenDropIds }
                                                .map { AnchorTarget(it.drop.id, it.drop.latitude, it.drop.longitude, it.drop.spatialAnchor) }
                                            anchorManager.update(session, earth, localized, targets)
                                            val positions = anchorManager.positions()
                                            if (positions.isNotEmpty() || targets.isEmpty()) lastResolvedAt = now
                                            positioningMessage = if (targets.isNotEmpty() && positions.isEmpty()) {
                                                when {
                                                    anchorManager.cloudError == com.google.ar.core.Anchor.CloudAnchorState.ERROR_NOT_AUTHORIZED ->
                                                        "Visual positioning authorization failed. Check the app's Google Cloud setup."
                                                    targets.any { it.spatialAnchor?.canResolveAt(System.currentTimeMillis()) == true } ->
                                                        "Scan nearby surfaces to recognize visual Drops…"
                                                    targets.any { it.spatialAnchor != null } ->
                                                        "Visual anchor expired. Use the map to find this Drop."
                                                    else -> "Finding outdoor positioning…"
                                                }
                                            } else null
                                            if (now - lastResolvedAt >= 60_000L) arCoreSupported = false

                                            if (now - lastFrameUpdate >= 33L && width > 0 && height > 0) {
                                                lastFrameUpdate = now
                                                val viewMatrix = FloatArray(16)
                                                val projectionMatrix = FloatArray(16)
                                                frame.camera.getViewMatrix(viewMatrix, 0)
                                                frame.camera.getProjectionMatrix(
                                                    projectionMatrix, 0, 0.1f,
                                                    LocationUtils.DISCOVERY_RADIUS + LocationUtils.HYSTERESIS_MARGIN + 50f
                                                )
                                                arFrame = SpatialFrame(
                                                    viewMatrix, projectionMatrix, frame.camera.pose.translation,
                                                    positions, width, height
                                                )
                                                val cameraPosition = frame.camera.pose.translation
                                                anchorManager.visualPositions().forEach { (id, point) ->
                                                    val distance = kotlin.math.sqrt(point.indices.sumOf { i ->
                                                        val delta = point[i] - cameraPosition[i]
                                                        (delta * delta).toDouble()
                                                    }).toFloat()
                                                    viewModel.onVisualDropResolved(id, distance)
                                                }
                                            }
                                        } else {
                                            trackingGate.reset()
                                            arFrame = null
                                            lastResolvedAt = now
                                            positioningMessage = "Look around to find your surroundings…"
                                            viewModel.onArTrackingLost()
                                            if (now - lastLocalizedAt >= 15_000L) arCoreSupported = false
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w(
                                    "DiscoveryScreen",
                                    "AR scene creation failed; switching to CameraX",
                                    e
                                )
                                arCoreSupported = false
                                android.view.View(ctx)
                            }
                        }
                    )
                    })
                    
                    val spatialFrame = arFrame
                    if (spatialFrame != null) {
                        val currentViewMatrix = spatialFrame.viewMatrix
                        val currentProjMatrix = spatialFrame.projectionMatrix
                        val cameraPos = spatialFrame.cameraPosition
                        val halfCardWidthPx = with(density) { 80.dp.toPx().toInt() }
                        val halfCardHeightPx = with(density) { 50.dp.toPx().toInt() }
                        var renderedOverlayCount = 0
                        val maxReadableArOverlays = com.somewhere.app.util.LocationUtils.MAX_VISIBLE

                        // Sort farther drops first so closer drops draw on top naturally
                        visibleDrops.sortedByDescending { it.distanceMeters }.forEach { item ->
                            if (renderedOverlayCount >= maxReadableArOverlays) {
                                return@forEach
                            }
                            if (item.drop.id in hiddenDropIds) return@forEach

                            val worldPos = spatialFrame.positions[item.drop.id]

                            if (worldPos != null) {
                                val screenPos = com.somewhere.app.util.ARUtils.projectToScreen(
                                    worldPos, currentViewMatrix, currentProjMatrix, spatialFrame.widthPx, spatialFrame.heightPx
                                )

                                if (screenPos != null) {
                                    renderedOverlayCount++

                                    val dx = cameraPos[0] - worldPos[0]
                                    val dy = cameraPos[1] - worldPos[1]
                                    val dz = cameraPos[2] - worldPos[2]
                                    val distance = kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)

                                    // Perspective scale from projection (closer = bigger, far = smaller)
                                    // Wider range than before so depth difference is clearly visible
                                    val perspectiveScale = DropVisuals.perspectiveScale(distance)

                                    // Atmospheric fade — far drops get slightly transparent (depth cue)
                                    val distanceAlpha = (1f - (distance - 5f) / 50f).coerceIn(0.4f, 1f)

                                    // Ground shadow projected below the card for spatial grounding
                                    val shadowScreenPos = com.somewhere.app.util.ARUtils.projectToScreen(
                                        floatArrayOf(worldPos[0], worldPos[1] - GeospatialAnchors.LABEL_HEIGHT_METERS.toFloat(), worldPos[2]),
                                        currentViewMatrix, currentProjMatrix, spatialFrame.widthPx, spatialFrame.heightPx
                                    )

                                    // Draw ground shadow first (behind the card)
                                    if (shadowScreenPos != null && item.drop.spatialAnchor == null) {
                                        val shadowScale = perspectiveScale * 0.6f
                                        Box(
                                            modifier = Modifier
                                                .offset {
                                                    androidx.compose.ui.unit.IntOffset(
                                                        shadowScreenPos.first.toInt() - (halfCardWidthPx * 0.5f).toInt(),
                                                        shadowScreenPos.second.toInt()
                                                    )
                                                }
                                                .graphicsLayer {
                                                    scaleX = shadowScale * 1.4f
                                                    scaleY = shadowScale * 0.3f
                                                    alpha = (0.3f * distanceAlpha).coerceIn(0f, 0.3f)
                                                }
                                                .size(120.dp, 40.dp)
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            Color.Black.copy(alpha = 0.5f),
                                                            Color.Transparent
                                                        )
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    // Draw the drop card — RAW coordinates, NO spring animation
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                androidx.compose.ui.unit.IntOffset(
                                                    screenPos.first.toInt() - halfCardWidthPx,
                                                    screenPos.second.toInt() - halfCardHeightPx
                                                )
                                            }
                                            .graphicsLayer {
                                                scaleX = perspectiveScale
                                                scaleY = perspectiveScale
                                                alpha = distanceAlpha
                                            }
                                    ) {
                                        if (item.isNewlyDiscovered) {
                                            PingPulse(modifier = Modifier.align(Alignment.Center))
                                        }
                                        DropOverlayCard(
                                            item = if (item.drop.spatialAnchor != null) item.copy(distanceMeters = distance) else item,
                                            onTap = {
                                                if (item.isUnlocked) viewModel.selectDrop(item)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    positioningMessage?.let { message ->
                        Text(
                            message,
                            color = SomewhereColors.TextPrimary,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .padding(bottom = 150.dp)
                                .background(SomewhereColors.Overlay, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )
                    }
                } else if (arCoreSupported == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SomewhereColors.Accent)
                    }
                } else if (arCoreSupported == false) {
                    // Fallback to CameraX
                    key(fallbackCameraRetryKey) {
                        val binding = remember { FallbackCameraBinding() }
                        AndroidView(
                            onRelease = { binding.release() },
                            factory = { ctx ->
                                val previewView = androidx.camera.view.PreviewView(ctx).apply {
                                    scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                                }

                                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener(listener@{
                                    if (binding.released) return@listener
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = androidx.camera.core.Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        binding.provider = cameraProvider
                                        binding.preview = preview
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview
                                        )
                                        fallbackCameraError = null
                                    } catch (e: Exception) {
                                        android.util.Log.w(
                                            "DiscoveryScreen",
                                            "CameraX fallback failed",
                                            e
                                        )
                                        fallbackCameraError = "Camera is unavailable. Close other camera apps and retry."
                                    }
                                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    fallbackCameraError?.let { error ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(SomewhereColors.Overlay, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                error,
                                color = SomewhereColors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            SomewhereButton(
                                text = "Retry Camera",
                                onClick = {
                                    fallbackCameraError = null
                                    fallbackCameraRetryKey++
                                }
                            )
                        }
                    }
                    
                    // Compass fallback overlay
                    if (visibleDrops.any { it.drop.spatialAnchor?.canResolveAt(System.currentTimeMillis()) == true }) {
                        TextButton(
                            onClick = { arAvailabilityCheck++ },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp)
                                .background(SomewhereColors.Overlay, RoundedCornerShape(12.dp))
                        ) { Text("Scan for visual Drops") }
                    }
                    val dropsInView = visibleDrops
                        .filter { kotlin.math.abs(it.angleDegrees) <= 32f }
                        .sortedWith(
                            compareByDescending<com.somewhere.app.viewmodel.DiscoveredDrop> {
                                it.isPrimary
                            }.thenBy { it.distanceMeters }
                        )
                        .take(6)

                    dropsInView.forEachIndexed { index, item ->
                        val pixelsPerDegree = screenWidthPx / 60f
                        val horizontalOffsetPx = (item.angleDegrees * pixelsPerDegree)
                            .coerceIn(-screenWidthPx * 0.38f, screenWidthPx * 0.38f)

                        val verticalOffsetDp = when (index) {
                            0 -> 0.dp
                            1 -> (-88).dp
                            2 -> 88.dp
                            3 -> (-176).dp
                            4 -> 176.dp
                            else -> 250.dp
                        }
                        val verticalOffsetPx = with(density) { verticalOffsetDp.toPx() }

                        val animatedHorizontalOffset by animateFloatAsState(
                            targetValue = horizontalOffsetPx,
                            animationSpec = spring(stiffness = 15f, dampingRatio = 0.9f),
                            label = "horizontalOffset"
                        )
                        
                        val animatedVerticalOffset by animateFloatAsState(
                            targetValue = verticalOffsetPx,
                            animationSpec = spring(stiffness = 15f, dampingRatio = 0.9f),
                            label = "verticalOffset"
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        x = animatedHorizontalOffset.toInt(),
                                        y = animatedVerticalOffset.toInt()
                                    )
                                }
                        ) {
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

                    if (visibleDrops.any { it.angleDegrees < -32f }) {
                        EdgeIndicator(
                            isLeft = true,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(bottom = 240.dp)
                        )
                    }
                    if (visibleDrops.any { it.angleDegrees > 32f }) {
                        EdgeIndicator(
                            isLeft = false,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(bottom = 240.dp)
                        )
                    }

                }
            } else {
                // List View Fallback
                Box(modifier = Modifier.fillMaxSize().background(SomewhereColors.Background)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!cameraPermission.status.isGranted) {
                            item {
                                Surface(
                                    color = SomewhereColors.Card,
                                    shape = RoundedCornerShape(2.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        SomewhereColors.CardBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "Camera view is optional",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = SomewhereColors.TextPrimary
                                        )
                                        Text(
                                            "The list still shows nearby Drops. Enable the camera only when you want the spatial view.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SomewhereColors.TextSecondary
                                        )
                                        TextButton(
                                            onClick = {
                                                openCameraAfterPermission = true
                                                cameraPermission.launchPermissionRequest()
                                            }
                                        ) {
                                            Text("Enable Camera")
                                        }
                                    }
                                }
                            }
                        }
                        items(visibleDrops) { item ->
                            DropOverlayCard(
                                item = item,
                                onTap = { if (item.isUnlocked) viewModel.selectDrop(item) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            // Empty state — animated radar pulse
            if (
                uiState.hasLocation &&
                visibleDrops.isEmpty() &&
                !uiState.isLoadingDrops &&
                uiState.loadError == null
            ) {
                RadarEmptyState(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Toggle view button (Top Right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isFoldTheme) SomewhereColors.GlassBackground else Color.Transparent)
                        .border(
                            0.5.dp,
                            if (isFoldTheme) SomewhereColors.GlassBorder else SomewhereColors.CardBorder,
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SomewhereColors.TextPrimary
                    )
                }
                Spacer(Modifier.weight(1f))
                
                IconButton(
                    onClick = {
                        if (showListView && !cameraPermission.status.isGranted) {
                            openCameraAfterPermission = true
                            cameraPermission.launchPermissionRequest()
                        } else {
                            showListView = !showListView
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isFoldTheme) SomewhereColors.GlassBackground else Color.Transparent)
                        .border(
                            0.5.dp,
                            if (isFoldTheme) SomewhereColors.GlassBorder else SomewhereColors.CardBorder,
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (showListView) Icons.Default.Map else Icons.Default.List,
                        contentDescription = if (showListView) "Open camera view" else "Open list view",
                        tint = ambient.pulseColor
                    )
                }
            }

            // Category Filter Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            val categories = com.somewhere.app.util.CategoryUtils.CATEGORIES
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
            ) {
                Surface(
                    color = if (isFoldTheme) SomewhereColors.GlassBackground else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isFoldTheme) SomewhereColors.GlassBorder else SomewhereColors.CardBorder
                    ),
                    onClick = { categoryExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.selectedCategory ?: "All Categories",
                            style = MaterialTheme.typography.labelMedium,
                            color = SomewhereColors.TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category",
                            modifier = Modifier.size(16.dp),
                            tint = SomewhereColors.TextPrimary
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.background(SomewhereColors.Card)
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories", color = SomewhereColors.TextPrimary) },
                        onClick = { 
                            viewModel.setCategoryFilter(null)
                            categoryExpanded = false 
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, color = SomewhereColors.TextPrimary) },
                            onClick = { 
                                viewModel.setCategoryFilter(category)
                                categoryExpanded = false 
                            }
                        )
                    }
                }
            }

            // Drop count badge
            if (uiState.hasLocation && visibleDrops.isNotEmpty()) {
                val unlockedCount = visibleDrops.count { it.isUnlocked }
                val totalCount = visibleDrops.size

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
                        SomewhereButton(
                            text = "Summary",
                            enabled = networkStatus == NetworkStatus.AVAILABLE && !uiState.isSummarizing,
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.summarizePlace() 
                            },
                            minWidth = 0.dp,
                            height = 48.dp,
                            modifier = Modifier.width(142.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ambient.pulseColor
                                )
                            }
                        )

                        SomewhereButton(
                            text = "Whisper",
                            enabled = networkStatus == NetworkStatus.AVAILABLE && !uiState.isCurating,
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.curateDrop() 
                            },
                            minWidth = 0.dp,
                            height = 48.dp,
                            modifier = Modifier.width(142.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Hearing,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ambient.pulseColor
                                )
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(SomewhereColors.GlassBackground)
                            .border(0.5.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
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

            if (networkStatus == NetworkStatus.UNAVAILABLE) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 76.dp),
                    color = SomewhereColors.Card,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offline — showing cached nearby Drops",
                            style = MaterialTheme.typography.bodySmall,
                            color = SomewhereColors.TextPrimary
                        )
                        TextButton(
                            onClick = { viewModel.setCategoryFilter(uiState.selectedCategory) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            if (!uiState.hasLocation) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.locationTimedOut) {
                        Text(
                            text = "Location timed out",
                            style = MaterialTheme.typography.titleMedium,
                            color = SomewhereColors.TextPrimary
                        )
                        Text(
                            text = "Move to an open area and check that precise location is enabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SomewhereColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        SomewhereButton(text = "Retry Location", onClick = viewModel::retryLocation)
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = SomewhereColors.Accent
                        )
                        Text(
                            text = "Getting your location…",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SomewhereColors.TextSecondary
                        )
                    }
                }
            } else if (uiState.loadError != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        uiState.loadError ?: "Could not load nearby Drops",
                        color = SomewhereColors.TextPrimary
                    )
                    SomewhereButton(
                        text = "Retry",
                        onClick = { viewModel.setCategoryFilter(uiState.selectedCategory) },
                        enabled = !uiState.isLoadingDrops
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            }

            // Detail overlay — shown when a drop is selected
            uiState.selectedDrop?.let { selected ->
                androidx.activity.compose.BackHandler { viewModel.selectDrop(null) }
                DropDetailSheet(
                    drop = selected.drop,
                    distanceMeters = selected.distanceMeters,
                    onDismiss = { viewModel.selectDrop(null) },
                    onDelete = {
                        val dropId = selected.drop.id
                        viewModel.selectDrop(null)
                        hiddenDropIds.add(dropId)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("Drop deleted", actionLabel = "Undo", duration = androidx.compose.material3.SnackbarDuration.Short)
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                hiddenDropIds.remove(dropId)
                            } else {
                                viewModel.deleteDrop(selected, showMessage = false)
                                hiddenDropIds.remove(dropId)
                            }
                        }
                    },
                    onReport = {
                        val dropId = selected.drop.id
                        viewModel.selectDrop(null)
                        hiddenDropIds.add(dropId)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("Drop reported", actionLabel = "Undo", duration = androidx.compose.material3.SnackbarDuration.Short)
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                hiddenDropIds.remove(dropId)
                            } else {
                                viewModel.reportDrop(selected, showMessage = false)
                                hiddenDropIds.remove(dropId)
                            }
                        }
                    },
                    onBlock = { 
                        selected.drop.authorName?.let { authorName ->
                            val dropId = selected.drop.id
                            viewModel.selectDrop(null)
                            hiddenDropIds.add(dropId)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar("User blocked", actionLabel = "Undo", duration = androidx.compose.material3.SnackbarDuration.Short)
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    hiddenDropIds.remove(dropId)
                                } else {
                                    viewModel.blockUser(authorName, showMessage = false)
                                    hiddenDropIds.remove(dropId)
                                }
                            }
                        }
                    },
                    onFindSpot = onFindSpot
                )
            }

            // Place Summary Dialog
            if (uiState.isSummarizing || uiState.summaryText != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearSummary() },
                    containerColor = SomewhereColors.Card,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor)
                            Spacer(Modifier.width(8.dp))
                            Text("Place Summary", color = SomewhereColors.TextPrimary)
                        }
                    },
                    text = {
                        if (uiState.isSummarizing) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Box(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                        } else {
                            Text(uiState.summaryText ?: "", color = SomewhereColors.TextSecondary)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearSummary() }) {
                            Text("Close", color = com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor)
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
                style = MaterialTheme.typography.labelMedium,
                color = com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor
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
                accuracy <= LocationUtils.ACCURACY_WARNING_METERS ->
                    "~${accuracy.toInt()}m accuracy"
                else -> "low · ~${accuracy.toInt()}m accuracy"
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
                        com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor.copy(alpha = 0.3f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(ring2Scale)
                    .alpha(ring2Alpha)
                    .background(
                        com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor.copy(alpha = 0.2f),
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
                    .background(com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor, CircleShape)
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

/** Unbind only this view's use case, including when its provider completes after disposal. */
private class FallbackCameraBinding {
    var released = false
        private set
    var provider: ProcessCameraProvider? = null
    var preview: Preview? = null

    fun release() {
        released = true
        preview?.let { useCase -> runCatching { provider?.unbind(useCase) } }
        preview = null
        provider = null
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
