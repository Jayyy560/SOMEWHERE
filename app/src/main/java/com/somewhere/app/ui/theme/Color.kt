package com.somewhere.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SOMEWHERE color palette.
 * Matte black + deep grays. No gradients. No bright accents.
 * Calm, precise, slightly mysterious.
 */
object SomewhereColors {
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF141414)
    val Card = Color(0xFF1E1E1E)
    val CardBorder = Color(0xFF2A2A2A)

    val TextPrimary = Color(0xFFEBEBEB)
    val TextSecondary = Color(0xFF808080)
    val TextMuted = Color(0xFF4A4A4A)

    val Accent = Color(0xFFF5F5F5)
    val AccentSubtle = Color(0xFFB0B0B0)
    val AccentPurple = Color(0xFFE0B0FF)

    val Overlay = Color(0xCC0A0A0A)     // 80% opacity black
    val OverlayLight = Color(0x660A0A0A) // 40% opacity black

    val Divider = Color(0xFF1A1A1A)

    val Error = Color(0xFFCF6679)

    // Glassmorphism overlay card
    val GlassBackground = Color(0xCC121218)      // Dark with slight blue tint, 80% opacity
    val GlassBorder = Color(0x33FFFFFF)           // 20% white border
    val GlowAccent = Color(0xFFE8D5B7)           // Warm amber glow for unlocked drops
    val GlowAccentDim = Color(0x33E8D5B7)        // Dim version for subtle radial glow
    val DistancePill = Color(0xFF2A2A3A)         // Muted pill background
    val DistancePillText = Color(0xFFD4C8B8)     // Warm off-white text for pills
    val LockTint = Color(0xFF5A5A6A)             // Subtle gray for locked state

    // Accuracy indicator colors
    val AccuracyGood = Color(0xFF4CAF50)
    val AccuracyModerate = Color(0xFFFF9800)
    val AccuracyPoor = Color(0xFFEF5350)
}
