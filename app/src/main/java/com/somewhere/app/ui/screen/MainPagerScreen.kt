package com.somewhere.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.somewhere.app.ui.component.FloatingBottomNav
import com.somewhere.app.ui.screen.DropScreen
import com.somewhere.app.ui.screen.HomeScreen
import com.somewhere.app.ui.screen.ProfileScreen
import com.somewhere.app.ui.screen.TripScreen
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraControl
import com.somewhere.app.ui.component.CameraBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainPagerScreen(
    onExplore: () -> Unit,
    onSettings: () -> Unit,
    onNotifications: () -> Unit,
    onFindSpot: (String) -> Unit
) {
    // Save the current page across process death so the user returns
    // to the exact tab they were on (e.g., Capture screen).
    var savedPage by rememberSaveable { mutableIntStateOf(1) }
    val pagerState = rememberPagerState(initialPage = savedPage) { 4 }
    val coroutineScope = rememberCoroutineScope()

    // Keep savedPage in sync with the pager
    LaunchedEffect(pagerState.currentPage) {
        savedPage = pagerState.currentPage
    }
    
    // Check if keyboard is visible reliably
    val isImeVisible = WindowInsets.isImeVisible

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 2) {
            focusRequester.requestFocus()
        }
    }

    // A callback we can trigger from either the screen button or volume keys
    var triggerCapture by remember { mutableStateOf(false) }
    
    var isCreatingDrop by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (pagerState.currentPage == 2 && event.type == KeyEventType.KeyUp) {
                    if (event.key == Key.VolumeUp || event.key == Key.VolumeDown) {
                        triggerCapture = true
                        return@onKeyEvent true
                    }
                }
                false
            }

    ) {
        // Z-Index 0: The Camera (only active when on or near the DropScreen to save battery and reduce lag)
        CameraBackground(
            isActive = pagerState.currentPage == 2 || pagerState.targetPage == 2,
            onImageCaptureCreated = { imageCapture = it },
            onCameraControlCreated = { cameraControl = it }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pagerState.currentPage != 0
        ) { page ->
            when (page) {
                0 -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    TripScreen(
                        onBack = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                }
                1 -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    HomeScreen(
                        onExplore = onExplore,
                        onSettings = onSettings,
                        onNotifications = onNotifications,
                        onTripMode = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    )
                }
                2 -> Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    DropScreen(
                        imageCapture = imageCapture,
                        triggerCapture = triggerCapture,
                        onCaptureTriggered = { triggerCapture = false },
                        onFocusTap = { nx, ny ->
                            val factory = androidx.camera.core.SurfaceOrientedMeteringPointFactory(1f, 1f)
                            val point = factory.createPoint(nx, ny)
                            val action = androidx.camera.core.FocusMeteringAction.Builder(point).build()
                            cameraControl?.startFocusAndMetering(action)
                        },
                        onComplete = {
                            // Return to Home tab
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        onCreationStateChanged = { isCreatingDrop = it }
                    )
                }
                3 -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    ProfileScreen(
                        onBack = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                }
            }
        }

        // Floating Bottom Navigation
        AnimatedVisibility(
            visible = !isImeVisible && !isCreatingDrop, // Hide when keyboard is open or when creating a drop
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingBottomNav(
                pagerState = pagerState,
                onTabSelected = { targetPage ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                },
                onProfileLongClick = { showAccountSwitcher = true }
            )
        }

        if (showAccountSwitcher) {
            com.somewhere.app.ui.component.AccountSwitcherSheet(
                onDismiss = { showAccountSwitcher = false },
                onAddAccount = { 
                    showAccountSwitcher = false
                    // Drop to auth screen is handled by MainActivity observing session status
                },
                onAccountSwitched = {
                    showAccountSwitcher = false
                }
            )
        }
    }
}
