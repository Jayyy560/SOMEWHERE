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
import androidx.compose.ui.draw.scale
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
import com.somewhere.app.ui.component.PermissionGate
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.component.shimmerEffect
import com.somewhere.app.ui.component.DropOverlayCard
import com.somewhere.app.ui.component.PingPulse
import com.somewhere.app.ui.component.TutorialOverlay
import com.somewhere.app.ui.component.SomewhereButton
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

    val hiddenDropIds = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val visibleDrops = uiState.nearbyDrops.filter { it.drop.id !in hiddenDropIds }
    
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

    val prefs = context.getSharedPreferences("somewhere_prefs", android.content.Context.MODE_PRIVATE)
    var showTutorial by remember { mutableStateOf(!prefs.getBoolean("has_seen_tutorial", false)) }

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
                    Lifecycle.Event.ON_STOP -> {
                        viewModel.stopTracking()
                        com.somewhere.app.util.ARUtils.resetCalibration()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { 
                lifecycleOwner.lifecycle.removeObserver(observer)
                com.somewhere.app.util.ARUtils.resetCalibration()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SomewhereColors.Background)
                .alpha(contentAlpha)
        ) {
            var showListView by remember { mutableStateOf(false) }

            // Fullscreen camera preview
            var arCoreSupported by remember { mutableStateOf<Boolean?>(null) }
            val activity = context as? android.app.Activity

            LaunchedEffect(Unit) {
                val apk = com.google.ar.core.ArCoreApk.getInstance()
                // Poll until the availability result is no longer transient (cold start can take >1s)
                var availability = apk.checkAvailability(context)
                var tries = 0
                while (availability.isTransient && tries < 15) {
                    kotlinx.coroutines.delay(200)
                    availability = apk.checkAvailability(context)
                    tries++
                }
                when (availability) {
                    com.google.ar.core.ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                        try {
                            // Test if the session can actually be created to catch false-positives
                            val session = com.google.ar.core.Session(context)
                            session.close()
                            arCoreSupported = true
                        } catch (e: Exception) {
                            arCoreSupported = false
                        }
                    }
                    com.google.ar.core.ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
                    com.google.ar.core.ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                        // Device supports AR but the ARCore app is missing/old — prompt to install.
                        if (activity != null) {
                            try {
                                when (apk.requestInstall(activity, true)) {
                                    com.google.ar.core.ArCoreApk.InstallStatus.INSTALLED ->
                                        arCoreSupported = true
                                    com.google.ar.core.ArCoreApk.InstallStatus.INSTALL_REQUESTED ->
                                        Unit // wait for resume
                                }
                            } catch (e: Exception) {
                                arCoreSupported = false
                            }
                        } else {
                            arCoreSupported = false
                        }
                    }
                    else -> {
                        // UNSUPPORTED_DEVICE_NOT_CAPABLE / UNKNOWN_ERROR / UNKNOWN_CHECKING / etc.
                        arCoreSupported = false
                    }
                }
            }

            var viewMatrix by remember { mutableStateOf<FloatArray?>(null) }
            var projMatrix by remember { mutableStateOf<FloatArray?>(null) }
            var cameraPos by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
            
            val configuration = LocalConfiguration.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.toInt()
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }.toInt()

            if (!showListView) {
                if (arCoreSupported == true) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            ARSceneView(ctx).apply {
                                planeRenderer.isVisible = false
                                configureSession { session, config ->
                                    config.geospatialMode = com.google.ar.core.Config.GeospatialMode.ENABLED
                                }
                                onSessionUpdated = { session, frame ->
                                    val cameraPose = frame.camera.pose
                                    
                                    val earth = session.earth
                                    val heading: Float
                                    if (earth?.trackingState == com.google.ar.core.TrackingState.TRACKING) {
                                        heading = earth.cameraGeospatialPose.heading.toFloat()
                                        viewModel.updateArCoreHeading(heading)
                                    } else {
                                        heading = viewModel.currentHeading
                                    }
                                    com.somewhere.app.util.ARUtils.calibrateNorth(cameraPose, heading)

                                    val state = viewModel.uiState.value
                                    val currentVisibleDrops = state.nearbyDrops.filter { it.drop.id !in hiddenDropIds }
                                    
                                    if (state.hasLocation && currentVisibleDrops.isNotEmpty()) {
                                        val drops = currentVisibleDrops.mapIndexed { index, item ->
                                            Pair(item.drop.id, Pair(item.drop.latitude, item.drop.longitude))
                                        }
                                        com.somewhere.app.util.ARUtils.recomputeAllPositions(
                                            session = session,
                                            cameraPose = cameraPose,
                                            userLat = state.userLat,
                                            userLon = state.userLon,
                                            drops = drops
                                        )
                                    }

                                    // Extract matrices for 2.5D projection
                                    // IMPORTANT: Create new array copies so Compose detects the state change
                                    val vMatrix = FloatArray(16)
                                    frame.camera.getViewMatrix(vMatrix, 0)
                                    viewMatrix = vMatrix.copyOf()
                                    
                                    val pMatrix = FloatArray(16)
                                    frame.camera.getProjectionMatrix(pMatrix, 0, 0.1f, 100f)
                                    projMatrix = pMatrix.copyOf()
                                    
                                    cameraPos = cameraPose.translation.copyOf()
                                }
                            }
                        }
                    )
                    
                    // 2.5D Overlay layer drawn ON TOP of the ARSceneView
                    val currentViewMatrix = viewMatrix
                    val currentProjMatrix = projMatrix
                    if (currentViewMatrix != null && currentProjMatrix != null) {
                        val halfCardWidthPx = with(density) { 80.dp.toPx().toInt() }
                        val halfCardHeightPx = with(density) { 50.dp.toPx().toInt() }

                        uiState.nearbyDrops.forEach { item ->
                            if (item.drop.id in hiddenDropIds) return@forEach

                            val worldPos = com.somewhere.app.util.ARUtils.getWorldPosition(item.drop.id)
                            android.util.Log.e("AR_DEBUG", "Drop ${item.drop.id.take(5)}: worldPos=${worldPos?.contentToString()}")
                            if (worldPos != null) {
                                val screenPos = com.somewhere.app.util.ARUtils.projectToScreen(
                                    worldPos, currentViewMatrix, currentProjMatrix, screenWidthPx, screenHeightPx
                                )
                                android.util.Log.e("AR_DEBUG", "Drop ${item.drop.id.take(5)}: screenPos=$screenPos, distanceAlpha...")
                                if (screenPos != null) {
                                    val dx = cameraPos[0] - worldPos[0]
                                    val dy = cameraPos[1] - worldPos[1]
                                    val dz = cameraPos[2] - worldPos[2]
                                    val distance = kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)

                                    // Perspective scale from projection (closer = bigger, far = smaller)
                                    // Wider range than before so depth difference is clearly visible
                                    val perspectiveScale = (3.5f / distance.coerceAtLeast(1f)).coerceIn(0.35f, 1.3f)

                                    // Atmospheric fade — far drops get slightly transparent (depth cue)
                                    val distanceAlpha = (1f - (distance - 5f) / 50f).coerceIn(0.4f, 1f)

                                    // Ground shadow projected below the card for spatial grounding
                                    val shadowScreenPos = com.somewhere.app.util.ARUtils.projectToScreen(
                                        floatArrayOf(worldPos[0], worldPos[1] - 0.8f, worldPos[2]),
                                        currentViewMatrix, currentProjMatrix, screenWidthPx, screenHeightPx
                                    )

                                    // Draw ground shadow first (behind the card)
                                    if (shadowScreenPos != null) {
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
                                            item = item,
                                            onTap = {
                                                if (item.isUnlocked) viewModel.selectDrop(item)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                } else if (arCoreSupported == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SomewhereColors.Accent)
                    }
                } else if (arCoreSupported == false) {
                    // Fallback to CameraX
                    AndroidView(
                        factory = { ctx ->
                            val previewView = androidx.camera.view.PreviewView(ctx).apply {
                                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Compass fallback overlay
                    val drops = visibleDrops
                    val zoneIndex = mutableMapOf<String, Int>()

                    drops.forEachIndexed { index, item ->
                        val pixelsPerDegree = screenWidthPx / 60f
                        val horizontalOffsetPx = item.angleDegrees * pixelsPerDegree

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

                }
            } else {
                // List View Fallback
                Box(modifier = Modifier.fillMaxSize().background(SomewhereColors.Background)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
            if (uiState.hasLocation && visibleDrops.isEmpty() && !showListView) {
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
                    onClick = { showListView = !showListView },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SomewhereColors.GlassBackground)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (showListView) Icons.Default.Map else Icons.Default.List,
                        contentDescription = "Toggle View",
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
                    color = SomewhereColors.GlassBackground,
                    shape = CircleShape,
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
                        Button(
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.summarizePlace() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SomewhereColors.GlassBackground),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(16.dp), tint = ambient.pulseColor)
                            Spacer(Modifier.width(6.dp))
                            Text("Summary", color = SomewhereColors.TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.curateDrop() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SomewhereColors.GlassBackground),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Hearing, contentDescription = "Whisper", modifier = Modifier.size(16.dp), tint = com.somewhere.app.ui.theme.LocalAmbientColors.current.pulseColor)
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
            
            // Tutorial Overlay
            if (showTutorial) {
                TutorialOverlay(onComplete = {
                    prefs.edit().putBoolean("has_seen_tutorial", true).apply()
                    showTutorial = false
                })
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
