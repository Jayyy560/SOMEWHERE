package com.somewhere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
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
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.ui.theme.SomewhereTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.serialization.json.booleanOrNull
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

val LocalPipMode = staticCompositionLocalOf { false }

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
        com.somewhere.app.util.NotificationHelper.checkAndRequestPermission(this)
        SupabaseManager.client.handleDeeplinks(intent)

        setContent {
            val pipMode by _isPipMode.collectAsState()
            CompositionLocalProvider(LocalPipMode provides pipMode) {
                SomewhereTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                    color = SomewhereColors.Background
                ) {
                    val sessionStatus by SupabaseManager.client.auth.sessionStatus
                        .collectAsState(initial = SessionStatus.NotAuthenticated(false))

                    when (val status = sessionStatus) {
                        is SessionStatus.Authenticated -> {
                            val session = status.session
                            val hasStrongPassword = session.user?.userMetadata
                                ?.get("has_strong_password")?.let { 
                                    if (it is kotlinx.serialization.json.JsonPrimitive) it.booleanOrNull else null 
                                } ?: false

                            if (hasStrongPassword) {
                                val navController = rememberNavController()
                                val startDest = com.somewhere.app.ui.navigation.NavDestinations.MAIN_PAGER
                                SomewhereNavGraph(navController = navController, startDestination = startDest)
                            } else {
                                com.somewhere.app.ui.screen.UpdatePasswordScreen(
                                    onPasswordUpdated = { },
                                    onSignOut = {
                                        lifecycleScope.launch {
                                            SupabaseManager.client.auth.signOut()
                                        }
                                    }
                                )
                            }
                        }
                        is SessionStatus.LoadingFromStorage -> {
                            // Show splash or blank while checking token
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                com.somewhere.app.ui.component.LiquidLogo()
                            }
                        }
                        else -> {
                            AuthScreen()
                        }
                    }
                    }
                }
            }
        }
    }
}
