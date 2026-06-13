package com.somewhere.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SOMEWHERE theme — dark only.
 * No dynamic color. No Material defaults showing through.
 */
private val SomewhereDarkColorScheme = darkColorScheme(
    primary = SomewhereColors.Accent,
    onPrimary = SomewhereColors.Background,
    secondary = SomewhereColors.AccentSubtle,
    onSecondary = SomewhereColors.Background,
    background = SomewhereColors.Background,
    onBackground = SomewhereColors.TextPrimary,
    surface = SomewhereColors.Surface,
    onSurface = SomewhereColors.TextPrimary,
    surfaceVariant = SomewhereColors.Card,
    onSurfaceVariant = SomewhereColors.TextSecondary,
    outline = SomewhereColors.CardBorder,
    error = SomewhereColors.Error,
    onError = SomewhereColors.Background
)

@Composable
fun SomewhereTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SomewhereColors.Background.toArgb()
            window.navigationBarColor = SomewhereColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SomewhereDarkColorScheme,
        typography = SomewhereTypography,
        content = content
    )
}
