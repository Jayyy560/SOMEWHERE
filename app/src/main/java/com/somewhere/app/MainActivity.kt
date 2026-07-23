package com.somewhere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import com.somewhere.app.data.location.TripManager
import com.somewhere.app.data.remote.SupabaseManager
import com.somewhere.app.ui.screen.AuthScreen
import com.somewhere.app.ui.navigation.SomewhereNavGraph
import com.somewhere.app.ui.theme.LocalAmbientColors
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.ui.theme.SomewhereTheme
import com.somewhere.app.ui.theme.rememberAmbientColors
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.serialization.json.booleanOrNull
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

val LocalPipMode = staticCompositionLocalOf { false }

/**
 * Stable auth categories — we use these instead of the raw SessionStatus so that
 * the composable tree for AUTHENTICATED is never torn down during the brief
 * LoadingFromStorage → Authenticated transition on process death.
 */
private enum class AuthCategory { LOADING, AUTHENTICATED, NEEDS_PASSWORD, NOT_AUTHENTICATED }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val _isPipMode = MutableStateFlow(false)

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isPipMode.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (TripManager.uiState.value.isNavigating) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(2, 3))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide system bars for immersive game-like mode
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        SupabaseManager.client.handleDeeplinks(intent)

        setContent {
            val pipMode by _isPipMode.collectAsState()
            val ambientColors = rememberAmbientColors()
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalPipMode provides pipMode,
                LocalAmbientColors provides ambientColors
            ) {
                SomewhereTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = SomewhereColors.Background
                    ) {
                        val sessionStatus by SupabaseManager.client.auth.sessionStatus
                            .collectAsState(initial = SessionStatus.LoadingFromStorage)

                        // Derive a stable auth category. Crucially, once we've been
                        // AUTHENTICATED we stay AUTHENTICATED during any transient
                        // LoadingFromStorage phase (which happens on process death).
                        // This prevents the NavGraph from being torn down & rebuilt,
                        // which would destroy all saved pager/screen state.
                        var authCategory by remember { mutableStateOf(AuthCategory.LOADING) }

                        LaunchedEffect(sessionStatus) {
                            authCategory = when (val status = sessionStatus) {
                                is SessionStatus.Authenticated -> {
                                    val hasStrong = status.session.user?.userMetadata
                                        ?.get("has_strong_password")?.let {
                                            if (it is kotlinx.serialization.json.JsonPrimitive) it.booleanOrNull else null
                                        } ?: false
                                    if (hasStrong) AuthCategory.AUTHENTICATED else AuthCategory.NEEDS_PASSWORD
                                }
                                is SessionStatus.LoadingFromStorage -> {
                                    // If we were already authenticated, stay that way
                                    // so the NavGraph isn't destroyed during the reload.
                                    if (authCategory == AuthCategory.AUTHENTICATED) {
                                        AuthCategory.AUTHENTICATED
                                    } else {
                                        AuthCategory.LOADING
                                    }
                                }
                                else -> AuthCategory.NOT_AUTHENTICATED
                            }
                        }

                        when (authCategory) {
                            AuthCategory.AUTHENTICATED -> {
                                val prefs = remember {
                                    getSharedPreferences(
                                        "somewhere_prefs",
                                        android.content.Context.MODE_PRIVATE
                                    )
                                }
                                var showOnboarding by remember {
                                    mutableStateOf(!prefs.getBoolean("has_seen_tutorial", false))
                                }
                                if (showOnboarding) {
                                    val finishOnboarding = {
                                        prefs.edit()
                                            .putBoolean("has_seen_tutorial", true)
                                            .apply()
                                        showOnboarding = false
                                    }
                                    com.somewhere.app.ui.component.TutorialOverlay(
                                        onComplete = finishOnboarding,
                                        onSkip = finishOnboarding
                                    )
                                } else {
                                    val startDest = com.somewhere.app.ui.navigation.NavDestinations.MAIN_PAGER
                                    SomewhereNavGraph(navController = navController, startDestination = startDest)
                                }
                            }
                            AuthCategory.NEEDS_PASSWORD -> {
                                com.somewhere.app.ui.screen.UpdatePasswordScreen(
                                    onPasswordUpdated = { },
                                    onSignOut = {
                                        lifecycleScope.launch {
                                            SupabaseManager.client.auth.signOut()
                                        }
                                    }
                                )
                            }
                            AuthCategory.LOADING -> {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    com.somewhere.app.ui.component.LiquidLogo()
                                }
                            }
                            AuthCategory.NOT_AUTHENTICATED -> {
                                AuthScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
