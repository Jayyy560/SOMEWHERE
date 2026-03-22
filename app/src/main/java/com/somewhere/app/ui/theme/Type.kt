package com.somewhere.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography system for SOMEWHERE.
 * System sans-serif with deliberate weight/tracking choices.
 * Light weights for elegance, generous letter-spacing for clarity.
 */
val SomewhereTypography = Typography(
    // App title — ultra-light, wide tracking
    displayLarge = TextStyle(
        fontWeight = FontWeight.W200,
        fontSize = 36.sp,
        letterSpacing = 12.sp,
        color = SomewhereColors.TextPrimary
    ),
    // Section headers
    headlineMedium = TextStyle(
        fontWeight = FontWeight.W300,
        fontSize = 20.sp,
        letterSpacing = 2.sp,
        color = SomewhereColors.TextPrimary
    ),
    // Card titles / overlay labels
    titleMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        letterSpacing = 0.5.sp,
        color = SomewhereColors.TextPrimary
    ),
    // Body text — message content
    bodyLarge = TextStyle(
        fontWeight = FontWeight.W300,
        fontSize = 15.sp,
        letterSpacing = 0.3.sp,
        lineHeight = 22.sp,
        color = SomewhereColors.TextPrimary
    ),
    // Secondary info — distance, timestamp
    bodySmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        color = SomewhereColors.TextSecondary
    ),
    // Button labels
    labelLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        letterSpacing = 3.sp,
        color = SomewhereColors.TextPrimary
    ),
    // Tiny labels — "move closer"
    labelSmall = TextStyle(
        fontWeight = FontWeight.W300,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = SomewhereColors.TextMuted
    )
)
