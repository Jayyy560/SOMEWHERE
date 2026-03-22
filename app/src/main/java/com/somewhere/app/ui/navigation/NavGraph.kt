package com.somewhere.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.somewhere.app.ui.screen.DiscoveryScreen
import com.somewhere.app.ui.screen.DropScreen
import com.somewhere.app.ui.screen.HomeScreen
import com.somewhere.app.ui.screen.SettingsScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val HOME = "home"
    const val DROP = "drop"
    const val DISCOVERY = "discovery"
    const val SETTINGS = "settings"
}

/**
 * Navigation graph — three screens, simple flat navigation.
 */
@Composable
fun SomewhereNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onExplore = { navController.navigate(Routes.DISCOVERY) },
                onDrop = { navController.navigate(Routes.DROP) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.DROP) {
            DropScreen(
                onComplete = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.DISCOVERY) {
            DiscoveryScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
