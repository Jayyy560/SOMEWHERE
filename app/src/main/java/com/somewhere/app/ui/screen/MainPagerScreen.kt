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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.somewhere.app.ui.component.FloatingBottomNav
import com.somewhere.app.ui.screen.DropScreen
import com.somewhere.app.ui.screen.HomeScreen
import com.somewhere.app.ui.screen.ProfileScreen
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
    onFindSpot: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val coroutineScope = rememberCoroutineScope()
    
    // Check if keyboard is visible reliably
    val isImeVisible = WindowInsets.isImeVisible

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            focusRequester.requestFocus()
        }
    }

    // A callback we can trigger from either the screen button or volume keys
    var triggerCapture by remember { mutableStateOf(false) }
    
    var isCreatingDrop by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (pagerState.currentPage == 1 && event.type == KeyEventType.KeyUp) {
                    if (event.key == Key.VolumeUp || event.key == Key.VolumeDown) {
                        triggerCapture = true
                        return@onKeyEvent true
                    }
                }
                false
            }

    ) {
        // Z-Index 0: The Camera (always pinned at the bottom to eliminate swipe jitter)
        CameraBackground(
            onImageCaptureCreated = { imageCapture = it },
            onCameraControlCreated = { cameraControl = it }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    HomeScreen(
                        onExplore = onExplore,
                        onSettings = onSettings,
                        // Pass empty functions since bottom nav handles switching now
                        onDrop = {},
                        onProfile = {}
                    )
                }
                1 -> Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
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
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        onCreationStateChanged = { isCreatingDrop = it }
                    )
                }
                2 -> Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    ProfileScreen(
                        // Tab doesn't need to pop back stack; just go to Home if they click back
                        onBack = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
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
                }
            )
        }
    }
}
