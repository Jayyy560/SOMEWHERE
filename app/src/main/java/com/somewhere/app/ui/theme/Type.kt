package com.somewhere.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Typography system for SOMEWHERE.
 *
 * The Home wordmark and the rest of the app intentionally share one sans-serif
 * family. The wordmark alone gets extreme tracking; headings use the same
 * light construction with natural spacing so they remain readable.
 */
val SomewhereSans = FontFamily.SansSerif

val SomewhereTypography = Typography(
    // Brand wordmark only.
    displayLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W200,
        fontSize = 36.sp,
        letterSpacing = 12.sp,
        color = LiquidPalette.textPrimary
    ),
    // Primary screen heading.
    displayMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = LiquidPalette.textPrimary
    ),
    displaySmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = 0.sp,
        color = LiquidPalette.textPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = LiquidPalette.textPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = LiquidPalette.textPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
        color = LiquidPalette.textPrimary
    ),
    // Large names and card-level headings.
    titleLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = 0.sp,
        color = LiquidPalette.textPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        color = LiquidPalette.textPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = LiquidPalette.textPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 23.sp,
        color = LiquidPalette.textPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 20.sp,
        color = LiquidPalette.textPrimary
    ),
    bodySmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.35.sp,
        lineHeight = 17.sp,
        color = LiquidPalette.textSecondary
    ),
    // Actions use tracking, but never the wordmark's exaggerated spacing.
    labelLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        letterSpacing = 2.sp,
        color = LiquidPalette.textPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.75.sp,
        color = LiquidPalette.textPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 11.sp,
        letterSpacing = 1.1.sp,
        color = LiquidPalette.textMuted
    )
)

val FoldTypography = SomewhereTypography.copy(
    displayLarge = SomewhereTypography.displayLarge.copy(
        fontWeight = FontWeight.W300,
        letterSpacing = 8.sp,
        color = Color.Unspecified
    ),
    displayMedium = SomewhereTypography.displayMedium.copy(
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.5).sp,
        color = Color.Unspecified
    ),
    displaySmall = SomewhereTypography.displaySmall.copy(
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.35).sp,
        color = Color.Unspecified
    ),
    headlineLarge = SomewhereTypography.headlineLarge.copy(
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.35).sp,
        color = Color.Unspecified
    ),
    headlineMedium = SomewhereTypography.headlineMedium.copy(
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.2).sp,
        color = Color.Unspecified
    ),
    headlineSmall = SomewhereTypography.headlineSmall.copy(color = Color.Unspecified),
    titleLarge = SomewhereTypography.titleLarge.copy(
        fontWeight = FontWeight.W500,
        color = Color.Unspecified
    ),
    titleMedium = SomewhereTypography.titleMedium.copy(color = Color.Unspecified),
    titleSmall = SomewhereTypography.titleSmall.copy(color = Color.Unspecified),
    bodyLarge = SomewhereTypography.bodyLarge.copy(color = Color.Unspecified),
    bodyMedium = SomewhereTypography.bodyMedium.copy(color = Color.Unspecified),
    bodySmall = SomewhereTypography.bodySmall.copy(color = Color.Unspecified),
    labelLarge = SomewhereTypography.labelLarge.copy(
        fontWeight = FontWeight.W500,
        letterSpacing = 1.25.sp,
        color = Color.Unspecified
    ),
    labelMedium = SomewhereTypography.labelMedium.copy(color = Color.Unspecified),
    labelSmall = SomewhereTypography.labelSmall.copy(color = Color.Unspecified)
)
