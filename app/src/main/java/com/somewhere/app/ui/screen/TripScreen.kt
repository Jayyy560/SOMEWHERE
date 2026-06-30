package com.somewhere.app.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.somewhere.app.LocalPipMode
import com.somewhere.app.data.remote.NearbyDrop
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.TripPhase
import com.somewhere.app.viewmodel.TripUiState
import com.somewhere.app.viewmodel.TripViewModel
import kotlinx.coroutines.launch

import androidx.core.util.Consumer
import androidx.activity.ComponentActivity
import android.app.Activity
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TripScreen(
    onBack: () -> Unit,
    tripViewModel: TripViewModel = viewModel(),
    discoveryViewModel: com.somewhere.app.viewmodel.DiscoveryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by tripViewModel.uiState.collectAsState()
    
    val activity = LocalContext.current as? Activity
    var isPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val listener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isPipMode = info.isInPictureInPictureMode
        }
        (activity as? ComponentActivity)?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            (activity as? ComponentActivity)?.removeOnPictureInPictureModeChangedListener(listener)
        }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Get current location
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var previewDrop by remember { mutableStateOf<NearbyDrop?>(null) }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        }
    }

        // Live location tracking
        DisposableEffect(locationPermission.status.isGranted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        currentLocation = latLng
                        tripViewModel.updateUserLocation(latLng)
                    }
                }
            }

            if (locationPermission.status.isGranted) {
                @SuppressLint("MissingPermission")
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
                client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
                
                @SuppressLint("MissingPermission")
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null && currentLocation == null) {
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        currentLocation = latLng
                        tripViewModel.updateUserLocation(latLng)
                    }
                }
            }

            onDispose {
                client.removeLocationUpdates(callback)
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
    ) {
        when (uiState.phase) {
            TripPhase.INPUT -> TripInputPhase(
                uiState = uiState,
                onPromptChange = { text, cursor -> tripViewModel.updatePrompt(text, cursor) },
                onSuggestionSelected = tripViewModel::selectSuggestion,
                onSearch = {
                    tripViewModel.searchRoute(
                        currentLat = currentLocation?.latitude,
                        currentLng = currentLocation?.longitude
                    )
                }
            )

            TripPhase.ROUTE_VIEW, TripPhase.NAVIGATING -> TripMapPhase(
                uiState = uiState,
                currentLocation = currentLocation,
                onBack = {
                    if (uiState.isNavigating) tripViewModel.stopNavigation()
                    else tripViewModel.resetTrip()
                },
                onDropSelected = tripViewModel::selectDrop,
                onStartNavigation = tripViewModel::startNavigation,
                onStopNavigation = tripViewModel::stopNavigation,
                onDismissApproaching = tripViewModel::dismissApproachingDrop,
                setPreviewDrop = { previewDrop = it }
            )
        }

        // Error snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SomewhereColors.Error,
                contentColor = Color.White,
                action = {
                    TextButton(onClick = { tripViewModel.clearError() }) {
                        Text("OK", color = Color.White)
                    }
                }
            ) {
                Text(error)
            }
        }
        
        // Preview Sheet
        previewDrop?.let { drop ->
            val mappedDrop = com.somewhere.app.data.model.Drop(
                id = drop.id,
                text = drop.text,
                imagePath = drop.imageUrl,
                latitude = drop.latitude,
                longitude = drop.longitude,
                timestamp = System.currentTimeMillis(), // or parse drop.createdAt if possible
                authorName = drop.authorName ?: "Anonymous",
                authorAvatarUrl = drop.authorAvatarUrl,
                authorId = drop.authorId ?: "",
                category = drop.category
            )

            DropDetailSheet(
                drop = mappedDrop,
                distanceMeters = 0f,
                onDismiss = { previewDrop = null },
                onDelete = {
                    discoveryViewModel.deleteDrop(
                        com.somewhere.app.viewmodel.DiscoveredDrop(
                            drop = mappedDrop,
                            distanceMeters = 0f,
                            isUnlocked = true,
                            angleDegrees = 0f
                        )
                    )
                    previewDrop = null
                },
                onReport = {
                    discoveryViewModel.reportDrop(
                        com.somewhere.app.viewmodel.DiscoveredDrop(
                            drop = mappedDrop,
                            distanceMeters = 0f,
                            isUnlocked = true,
                            angleDegrees = 0f
                        )
                    )
                    previewDrop = null
                },
                onBlock = {
                    discoveryViewModel.blockUser(mappedDrop.authorName ?: "")
                    previewDrop = null
                },
                onFindSpot = { 
                    previewDrop = null
                    tripViewModel.routeToDrop(drop, currentLocation?.latitude, currentLocation?.longitude)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PHASE 1: Input Screen — origin, destination, and query
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TripInputPhase(
    uiState: TripUiState,
    onPromptChange: (String, Int) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onSearch: () -> Unit
) {
    val scrollState = rememberScrollState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "contentAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition()
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = (2 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), 
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val isImeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val topSpacerHeight by animateDpAsState(
        targetValue = if (isImeVisible) 24.dp else (screenHeight / 2) - 150.dp,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "topSpacer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .alpha(contentAlpha)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .imePadding()
    ) {
        Spacer(modifier = Modifier.height(topSpacerHeight))

        // ── Header: WAYFINDER ──
        Text(
            text = "WAYFINDER",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.W200,
                fontSize = 36.sp,
                letterSpacing = 12.sp
            ),
            color = SomewhereColors.TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── Iridescent Glass Search Bar with Send Button ──
        val shape = if (uiState.isLoading) com.somewhere.app.ui.component.WavyPillShape(waveProgress, 2.5f) else RoundedCornerShape(percent = 50)
        
        val primaryGlow by androidx.compose.animation.animateColorAsState(
            targetValue = if (uiState.isLoading) Color(0xFF80D8FF) else Color.Transparent,
            label = "primaryGlow"
        )
        val secondaryGlow by androidx.compose.animation.animateColorAsState(
            targetValue = if (uiState.isLoading) Color(0xFFEA80FC) else Color.Transparent,
            label = "secondaryGlow"
        )
        val shadowAmbient by androidx.compose.animation.animateColorAsState(
            targetValue = if (uiState.isLoading) Color(0xFF00E5FF) else Color.Black,
            label = "shadowAmbient"
        )
        val shadowSpot by androidx.compose.animation.animateColorAsState(
            targetValue = if (uiState.isLoading) Color(0xFFFF00FF) else Color.Black,
            label = "shadowSpot"
        )
        
        var textValue by remember { 
            mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(uiState.promptText)) 
        }

        LaunchedEffect(uiState.promptText) {
            if (uiState.promptText != textValue.text) {
                textValue = textValue.copy(
                    text = uiState.promptText,
                    selection = androidx.compose.ui.text.TextRange(uiState.promptText.length)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (uiState.isLoading) 24.dp else 12.dp,
                    shape = shape,
                    ambientColor = shadowAmbient.copy(alpha = 0.2f),
                    spotColor = shadowSpot.copy(alpha = 0.3f)
                )
                .clip(shape)
                .background(Color.White.copy(alpha = 0.02f))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.2f), 
                            Color.Transparent
                        ),
                        center = Offset(100f, 50f),
                        radius = 300f
                    )
                )
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            secondaryGlow.copy(alpha = 0.15f), 
                            Color.Transparent
                        ),
                        center = Offset(500f, 150f),
                        radius = 400f
                    )
                )
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.4f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            primaryGlow.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.1f),
                            secondaryGlow.copy(alpha = 0.5f),
                            Color.White
                        )
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (textValue.text.isEmpty()) {
                        Text(
                            "Discover Drops",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            textValue = newValue
                            onPromptChange(newValue.text, newValue.selection.start)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSearch() }
                        )
                    )
                }

                // Liquid Send Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SomewhereColors.GlassBackground)
                        .border(BorderStroke(0.5.dp, SomewhereColors.GlassBorder), CircleShape)
                        .clickable { onSearch() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Suggestions Dropdown (appears right under the search bar)
        AnimatedVisibility(visible = uiState.destinationSuggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SomewhereColors.GlassBackground)
                    .border(0.5.dp, SomewhereColors.GlassBorder, RoundedCornerShape(16.dp))
            ) {
                uiState.destinationSuggestions.forEachIndexed { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionSelected(suggestion) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = SomewhereColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = suggestion,
                            color = SomewhereColors.TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    if (index < uiState.destinationSuggestions.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Quick Searches ──
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val suggestions = listOf(
                "Best food spots",
                "Photo worthy",
                "Hidden gems",
                "Live music",
                "Cafes",
                "Historic sites"
            )
            items(suggestions) { suggestion ->
                SuggestionChip(
                    onClick = { 
                        val newPrompt = if (textValue.text.isBlank()) suggestion else "${textValue.text} $suggestion"
                        onPromptChange(newPrompt, newPrompt.length)
                    },
                    label = {
                        Text(
                            suggestion,
                            fontSize = 13.sp,
                            color = SomewhereColors.TextPrimary
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.Transparent
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PHASE 2: Map View — route polyline + drops + navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberPipMode(): Boolean {
    val activity = LocalContext.current as? Activity
    var isPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val listener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isPipMode = info.isInPictureInPictureMode
        }
        (activity as? ComponentActivity)?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            (activity as? ComponentActivity)?.removeOnPictureInPictureModeChangedListener(listener)
        }
    }
    return isPipMode
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun TripMapPhase(
    uiState: TripUiState,
    currentLocation: com.google.android.gms.maps.model.LatLng?,
    onBack: () -> Unit,
    onDropSelected: (NearbyDrop?) -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onDismissApproaching: () -> Unit,
    setPreviewDrop: (NearbyDrop) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()
    val isPipMode = rememberPipMode()

    if (isPipMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                ),
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                // Draw route polyline
                if (uiState.routePolyline.isNotEmpty()) {
                    Polyline(
                        points = uiState.routePolyline,
                        color = SomewhereColors.GlowAccent,
                        width = 12f
                    )
                    Polyline(
                        points = uiState.routePolyline,
                        color = SomewhereColors.GlowAccentDim,
                        width = 24f
                    )
                }

                // Follow user location when navigating
                LaunchedEffect(uiState.isNavigating, currentLocation) {
                    if (uiState.isNavigating && currentLocation != null) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(currentLocation!!, 16f)
                            ),
                        durationMs = 1000
                        )
                    }
                }
            }
            // Add a small overlay for PiP mode showing the next instruction
            if (uiState.routeSummary.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.routeSummary,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.routeDistance.isNotEmpty()) {
                            Text(
                                text = uiState.routeDistance,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Fit the camera to the route bounds on first load
    LaunchedEffect(uiState.routePolyline) {
        if (uiState.routePolyline.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            uiState.routePolyline.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100),
                durationMs = 800
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(bottom = 200.dp, top = 100.dp),
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = true
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            // Draw route polyline
            if (uiState.routePolyline.isNotEmpty()) {
                Polyline(
                    points = uiState.routePolyline,
                    color = SomewhereColors.GlowAccent,
                    width = 12f
                )
                // Subtle outer glow polyline
                Polyline(
                    points = uiState.routePolyline,
                    color = SomewhereColors.GlowAccentDim,
                    width = 24f
                )
            }

            // Follow user location when navigating
            LaunchedEffect(uiState.isNavigating, currentLocation) {
                if (uiState.isNavigating && currentLocation != null) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(currentLocation!!, 16f)
                        ),
                    durationMs = 1000
                    )
                }
            }

            // Rerouting Indicator
            if (uiState.isRecalculating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                        .background(SomewhereColors.GlassBackground, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = SomewhereColors.GlowAccent,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rerouting...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Drop markers along route
            uiState.filteredDrops.forEach { drop ->
                val position = LatLng(drop.latitude, drop.longitude)
                Marker(
                    state = MarkerState(position = position),
                    title = drop.text.take(40),
                    snippet = drop.category ?: "Drop",
                    onClick = {
                        onDropSelected(drop)
                        false
                    }
                )
            }
        }

        if (isPipMode) {
            // PIP Mode Minimal UI
            // Purposely leaving this empty. We just hide the large UI elements below so the map gets 100% of the PiP window.
        }

        if (!isPipMode) {
            // ── Top Bar Overlay ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = SomewhereColors.GlassBackground
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SomewhereColors.TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Route info pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SomewhereColors.GlassBackground,
                    border = BorderStroke(0.5.dp, SomewhereColors.GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = SomewhereColors.GlowAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            uiState.routeDuration,
                            color = SomewhereColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("•", color = SomewhereColors.TextMuted)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            uiState.routeDistance,
                            color = SomewhereColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ── Bottom Info Panel ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 110.dp)
            ) {
                // Drop count pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = SomewhereColors.GlassBackground,
                    border = BorderStroke(0.5.dp, SomewhereColors.GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = SomewhereColors.GlowAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${uiState.filteredDrops.size} drops found on route",
                            color = SomewhereColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Drop carousel (horizontal scroll of drop cards)
                if (uiState.filteredDrops.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredDrops) { drop ->
                            NearbyDropItem(
                                drop = drop,
                                isSelected = uiState.selectedDrop?.id == drop.id,
                                onClick = {
                                    onDropSelected(drop)
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.fromLatLngZoom(
                                                    LatLng(drop.latitude, drop.longitude),
                                                    15f
                                                )
                                            ),
                                            durationMs = 600
                                        )
                                    }
                                },
                                onLongClick = { setPreviewDrop(drop) }
                            )
                        }
                    }
                }

                // Navigation button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.isNavigating) SomewhereColors.Error
                            else SomewhereColors.GlowAccent
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (uiState.isNavigating) onStopNavigation()
                                else onStartNavigation()
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (uiState.isNavigating) Icons.Default.Stop
                            else Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (uiState.isNavigating) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (uiState.isNavigating) "STOP TRIP" else "START TRIP",
                            color = if (uiState.isNavigating) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // ── Approaching Drop Alert ──
            AnimatedVisibility(
                visible = uiState.approachingDrop != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                uiState.approachingDrop?.let { drop ->
                    ApproachingDropAlert(
                        drop = drop,
                        onDismiss = onDismissApproaching,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 72.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Components
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NearbyDropItem(
    drop: NearbyDrop,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = SomewhereColors.Card,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 0.5.dp,
            if (isSelected) SomewhereColors.GlowAccent else SomewhereColors.CardBorder
        )
    ) {
        Box {
            // Background image
            AsyncImage(
                model = drop.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 40f
                        )
                    )
            )

            // Text content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    drop.text.take(50),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (drop.category != null) {
                        Text(
                            drop.category,
                            color = SomewhereColors.GlowAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        drop.authorName ?: "Anonymous",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ApproachingDropAlert(
    drop: NearbyDrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SomewhereColors.Card,
        border = BorderStroke(1.dp, SomewhereColors.GlowAccent.copy(alpha = glowAlpha)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drop image
            AsyncImage(
                model = drop.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "APPROACHING DROP",
                    color = SomewhereColors.GlowAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    drop.text.take(60),
                    color = SomewhereColors.TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (drop.authorName != null) {
                    Text(
                        "by ${drop.authorName}",
                        color = SomewhereColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = SomewhereColors.TextSecondary
                )
            }
        }
    }
}
