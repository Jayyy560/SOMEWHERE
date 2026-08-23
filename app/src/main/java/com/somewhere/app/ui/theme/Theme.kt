package com.somewhere.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SOMEWHERE theme — dark only.
 * No dynamic color. No Material defaults showing through.
 */
private val SomewhereDarkColorScheme = darkColorScheme(
    primary = LiquidPalette.accent,
    onPrimary = LiquidPalette.background,
    secondary = LiquidPalette.accentSubtle,
    onSecondary = LiquidPalette.background,
    background = LiquidPalette.background,
    onBackground = LiquidPalette.textPrimary,
    surface = LiquidPalette.surface,
    onSurface = LiquidPalette.textPrimary,
    surfaceVariant = LiquidPalette.card,
    onSurfaceVariant = LiquidPalette.textSecondary,
    outline = LiquidPalette.cardBorder,
    error = LiquidPalette.error,
    onError = LiquidPalette.background
)

private val FoldColorScheme = darkColorScheme(
    primary = FoldPalette.accent,
    onPrimary = FoldPalette.surface,
    secondary = FoldPalette.accentSubtle,
    onSecondary = FoldPalette.surface,
    background = FoldPalette.background,
    onBackground = FoldPalette.textPrimary,
    surface = FoldPalette.surface,
    onSurface = FoldPalette.textPrimary,
    surfaceVariant = FoldPalette.card,
    onSurfaceVariant = FoldPalette.textSecondary,
    outline = FoldPalette.cardBorder,
    error = FoldPalette.error,
    onError = FoldPalette.surface
)

private val FoldShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun SomewhereTheme(
    theme: AppTheme = AppTheme.LIQUID,
    content: @Composable () -> Unit
) {
    SomewhereColors.activate(theme)
    val isFold = theme == AppTheme.FOLD
    val colorScheme = if (isFold) FoldColorScheme else SomewhereDarkColorScheme
    val typography = if (isFold) FoldTypography else SomewhereTypography
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        if (isFold) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = FoldShapes,
                content = content
            )
        } else {
            // Keep the original MaterialTheme call untouched for the default skin.
            MaterialTheme(
                colorScheme = SomewhereDarkColorScheme,
                typography = SomewhereTypography,
                content = content
            )
        }
    }
}
