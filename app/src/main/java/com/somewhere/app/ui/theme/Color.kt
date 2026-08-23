package com.somewhere.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class SomewherePalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSubtle: Color,
    val accentPurple: Color,
    val overlay: Color,
    val overlayLight: Color,
    val divider: Color,
    val error: Color,
    val glassBackground: Color,
    val glassBorder: Color,
    val glowAccent: Color,
    val glowAccentDim: Color,
    val distancePill: Color,
    val distancePillText: Color,
    val lockTint: Color,
    val accuracyGood: Color,
    val accuracyModerate: Color,
    val accuracyPoor: Color
)

internal val LiquidPalette = SomewherePalette(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    card = Color(0xFF1E1E1E),
    cardBorder = Color(0xFF2A2A2A),
    textPrimary = Color(0xFFEBEBEB),
    textSecondary = Color(0xFF808080),
    textMuted = Color(0xFF7A7A7A),
    accent = Color(0xFFF5F5F5),
    accentSubtle = Color(0xFFB0B0B0),
    accentPurple = Color(0xFFE0B0FF),
    overlay = Color(0xCC0A0A0A),
    overlayLight = Color(0x660A0A0A),
    divider = Color(0xFF1A1A1A),
    error = Color(0xFFCF6679),
    glassBackground = Color(0xCC121218),
    glassBorder = Color(0x33FFFFFF),
    glowAccent = Color(0xFFE8D5B7),
    glowAccentDim = Color(0x33E8D5B7),
    distancePill = Color(0xFF2A2A3A),
    distancePillText = Color(0xFFD4C8B8),
    lockTint = Color(0xFF5A5A6A),
    accuracyGood = Color(0xFF4CAF50),
    accuracyModerate = Color(0xFFFF9800),
    accuracyPoor = Color(0xFFEF5350)
)

internal val FoldPalette = SomewherePalette(
    background = Color(0xFF090909),
    surface = Color(0xFF11110F),
    card = Color(0xFF1B1A17),
    cardBorder = Color(0xFF3D3A34),
    textPrimary = Color(0xFFF0ECE2),
    textSecondary = Color(0xFFB5AEA0),
    textMuted = Color(0xFF777268),
    accent = Color(0xFFF0ECE2),
    accentSubtle = Color(0xFFB5AEA0),
    accentPurple = Color(0xFFD3C6AA),
    overlay = Color(0xD9090909),
    overlayLight = Color(0x73090909),
    divider = Color(0xFF292722),
    error = Color(0xFFFF7D73),
    glassBackground = Color(0xEB11110F),
    glassBorder = Color(0x66F0ECE2),
    glowAccent = Color(0xFFD3C6AA),
    glowAccentDim = Color(0x44D3C6AA),
    distancePill = Color(0xFF1B1A17),
    distancePillText = Color(0xFFF0ECE2),
    lockTint = Color(0xFF98958B),
    accuracyGood = Color(0xFF78C995),
    accuracyModerate = Color(0xFFFFB85C),
    accuracyPoor = Color(0xFFFF7D73)
)

/**
 * SOMEWHERE color palette.
 * Matte black + deep grays. No gradients. No bright accents.
 * Calm, precise, slightly mysterious.
 */
object SomewhereColors {
    private var activePalette: SomewherePalette by mutableStateOf(LiquidPalette)

    internal fun activate(theme: AppTheme) {
        activePalette = if (theme == AppTheme.FOLD) FoldPalette else LiquidPalette
    }

    val Background get() = activePalette.background
    val Surface get() = activePalette.surface
    val Card get() = activePalette.card
    val CardBorder get() = activePalette.cardBorder
    val TextPrimary get() = activePalette.textPrimary
    val TextSecondary get() = activePalette.textSecondary
    val TextMuted get() = activePalette.textMuted
    val Accent get() = activePalette.accent
    val AccentSubtle get() = activePalette.accentSubtle
    val AccentPurple get() = activePalette.accentPurple
    val Overlay get() = activePalette.overlay
    val OverlayLight get() = activePalette.overlayLight
    val Divider get() = activePalette.divider
    val Error get() = activePalette.error
    val GlassBackground get() = activePalette.glassBackground
    val GlassBorder get() = activePalette.glassBorder
    val GlowAccent get() = activePalette.glowAccent
    val GlowAccentDim get() = activePalette.glowAccentDim
    val DistancePill get() = activePalette.distancePill
    val DistancePillText get() = activePalette.distancePillText
    val LockTint get() = activePalette.lockTint
    val AccuracyGood get() = activePalette.accuracyGood
    val AccuracyModerate get() = activePalette.accuracyModerate
    val AccuracyPoor get() = activePalette.accuracyPoor
}
