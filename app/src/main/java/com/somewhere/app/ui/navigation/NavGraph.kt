package com.somewhere.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.somewhere.app.ui.screen.DiscoveryScreen
import com.somewhere.app.ui.screen.DropScreen
import com.somewhere.app.ui.screen.HomeScreen
import com.somewhere.app.ui.screen.SettingsScreen
import com.somewhere.app.ui.screen.ProfileScreen
import com.somewhere.app.ui.screen.FindSpotScreen

/**
 * Navigation routes for the app.
 */
object NavDestinations {
    const val HOME = "home"
    const val CREATE_DROP = "create_drop"
    const val DISCOVERY = "discovery"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val FIND_SPOT = "find_spot/{imageUrl}"
    
    fun createFindSpotRoute(imageUrl: String): String {
        return "find_spot/${java.net.URLEncoder.encode(imageUrl, "UTF-8")}"
    }
}

private const val TRANSITION_DURATION = 350

/**
 * Navigation graph — four screens with smooth slide transitions.
 */
@Composable
fun SomewhereNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavDestinations.HOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(TRANSITION_DURATION)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
        }
    ) {
        composable(NavDestinations.HOME) {
            HomeScreen(
                onExplore = { navController.navigate(NavDestinations.DISCOVERY) },
                onDrop = { navController.navigate(NavDestinations.CREATE_DROP) },
                onSettings = { navController.navigate(NavDestinations.SETTINGS) },
                onProfile = { navController.navigate(NavDestinations.PROFILE) }
            )
        }

        composable(NavDestinations.CREATE_DROP) {
            DropScreen(
                onComplete = {
                    navController.popBackStack(NavDestinations.HOME, inclusive = false)
                }
            )
        }

        composable(NavDestinations.DISCOVERY) {
            DiscoveryScreen(
                onFindSpot = { imageUrl ->
                    navController.navigate(NavDestinations.createFindSpotRoute(imageUrl))
                }
            )
        }

        composable(NavDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavDestinations.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavDestinations.FIND_SPOT) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
            val imageUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            FindSpotScreen(
                originalImageUrl = imageUrl,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
