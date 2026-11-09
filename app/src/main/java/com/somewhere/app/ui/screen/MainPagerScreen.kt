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
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraControl
import com.somewhere.app.ui.component.CameraBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import com.somewhere.app.ui.theme.AppTheme
import com.somewhere.app.ui.theme.LocalAppTheme
import com.somewhere.app.ui.theme.SomewhereColors

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainPagerScreen(
    onExplore: () -> Unit,
    onSettings: () -> Unit,
    onNotifications: () -> Unit,
    onFindSpot: (String) -> Unit
) {
    val isFoldTheme = LocalAppTheme.current == AppTheme.FOLD
    val pages = remember {
        buildList {
            if (com.somewhere.app.PrototypeFeatures.SHOW_WAYFINDER) add(MainPage.WAYFINDER)
            add(MainPage.HOME)
            add(MainPage.CREATE)
            add(MainPage.PROFILE)
        }
    }
    val homePage = pages.indexOf(MainPage.HOME)
    val createPage = pages.indexOf(MainPage.CREATE)

    // Versioned input prevents an old four-tab index from restoring into the
    // prototype's smaller navigation model.
    var savedPage by rememberSaveable("full_nav_v1") { mutableIntStateOf(homePage) }
    val pagerState = rememberPagerState(
        initialPage = savedPage.coerceIn(pages.indices)
    ) { pages.size }
    val coroutineScope = rememberCoroutineScope()

    // Keep savedPage in sync with the pager
    LaunchedEffect(pagerState.currentPage) {
        savedPage = pagerState.currentPage
    }
    
    // Check if keyboard is visible reliably
    val isImeVisible = WindowInsets.isImeVisible

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraRetryKey by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == createPage) {
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
                if (pagerState.currentPage == createPage && event.type == KeyEventType.KeyUp) {
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
            isActive = !isCreatingDrop && (pagerState.currentPage == createPage || pagerState.targetPage == createPage),
            onImageCaptureCreated = { imageCapture = it },
            onCameraControlCreated = { cameraControl = it },
            onCameraError = { cameraError = it },
            retryKey = cameraRetryKey
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pagerState.currentPage != 0 && !isCreatingDrop
        ) { pageIndex ->
            val pageOffset = (
                (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            ).coerceIn(-1f, 1f)
            val pageModifier = if (isFoldTheme) {
                Modifier.graphicsLayer {
                    rotationY = pageOffset * -12f
                    cameraDistance = 28f * density
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (pageOffset < 0f) 0f else 1f,
                        pivotFractionY = 0.5f
                    )
                    alpha = 1f - kotlin.math.abs(pageOffset) * 0.12f
                }
            } else {
                Modifier
            }
            when (pages[pageIndex]) {
                MainPage.WAYFINDER -> Box(modifier = pageModifier.fillMaxSize().background(if (isFoldTheme) SomewhereColors.Background else Color.Black)) {
                    TripScreen(
                        isActive = pagerState.currentPage == pageIndex,
                        onBack = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(homePage)
                            }
                        }
                    )
                }
                MainPage.HOME -> Box(modifier = pageModifier.fillMaxSize().background(if (isFoldTheme) SomewhereColors.Background else Color.Black)) {
                    HomeScreen(
                        onExplore = onExplore,
                        onSettings = onSettings,
                        onNotifications = onNotifications,
                        onTripMode = {
                            if (com.somewhere.app.PrototypeFeatures.SHOW_WAYFINDER) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pages.indexOf(MainPage.WAYFINDER))
                                }
                            }
                        }
                    )
                }
                MainPage.CREATE -> Box(modifier = pageModifier.fillMaxSize().background(Color.Transparent)) {
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
                                pagerState.animateScrollToPage(homePage)
                            }
                        },
                        cameraError = cameraError,
                        onRetryCamera = {
                            cameraError = null
                            imageCapture = null
                            cameraRetryKey++
                        },
                        onCreationStateChanged = { isCreatingDrop = it }
                    )
                }
                MainPage.PROFILE -> Box(modifier = pageModifier.fillMaxSize().background(if (isFoldTheme) SomewhereColors.Background else Color.Black)) {
                    ProfileScreen(
                        onExplore = onExplore,
                        onBack = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(homePage)
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

private enum class MainPage {
    WAYFINDER,
    HOME,
    CREATE,
    PROFILE
}
