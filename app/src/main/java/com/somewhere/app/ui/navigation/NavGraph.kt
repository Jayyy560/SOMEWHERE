package com.somewhere.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.somewhere.app.ui.screen.DiscoveryScreen
import com.somewhere.app.ui.screen.HomeScreen
import com.somewhere.app.ui.screen.SettingsScreen
import com.somewhere.app.ui.screen.FindSpotScreen
import com.somewhere.app.ui.screen.MainPagerScreen

/**
 * Navigation routes for the app.
 */
object NavDestinations {
    const val MAIN_PAGER = "main_pager"
    const val DISCOVERY = "discovery"
    const val SETTINGS = "settings"
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
        startDestination = NavDestinations.MAIN_PAGER,
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
        composable(NavDestinations.MAIN_PAGER) {
            MainPagerScreen(
                onExplore = { navController.navigate(NavDestinations.DISCOVERY) },
                onSettings = { navController.navigate(NavDestinations.SETTINGS) },
                onFindSpot = { imageUrl ->
                    navController.navigate(NavDestinations.createFindSpotRoute(imageUrl))
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
