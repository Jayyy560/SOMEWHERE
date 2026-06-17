package com.somewhere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.somewhere.app.util.NotificationHelper.checkAndRequestPermission(this)
        SupabaseManager.client.handleDeeplinks(intent)

        setContent {
            SomewhereTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SomewhereColors.Background
                ) {
                    val sessionStatus by SupabaseManager.client.auth.sessionStatus
                        .collectAsState(initial = SessionStatus.NotAuthenticated(false))

                    if (sessionStatus is SessionStatus.Authenticated) {
                        val session = (sessionStatus as SessionStatus.Authenticated).session
                        val hasStrongPassword = session.user?.userMetadata
                            ?.get("has_strong_password")?.let { 
                                if (it is kotlinx.serialization.json.JsonPrimitive) it.booleanOrNull else null 
                            } ?: false

                        if (hasStrongPassword) {
                            val navController = rememberNavController()
                            SomewhereNavGraph(navController = navController)
                        } else {
                            com.somewhere.app.ui.screen.UpdatePasswordScreen(
                                onPasswordUpdated = {
                                    // It will trigger a re-render as session might update, 
                                    // or we could just force a refresh. The best way is 
                                    // to wait for auth state change or manually force refresh
                                    // Since we update user, the sessionStatus might automatically emit a new state
                                },
                                onSignOut = {
                                    lifecycleScope.launch {
                                        SupabaseManager.client.auth.signOut()
                                    }
                                }
                            )
                        }
                    } else {
                        AuthScreen()
                    }
                }
            }
        }
    }
}
