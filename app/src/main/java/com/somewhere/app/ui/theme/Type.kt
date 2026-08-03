package com.somewhere.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
        color = SomewhereColors.TextPrimary
    ),
    // Primary screen heading.
    displayMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = SomewhereColors.TextPrimary
    ),
    displaySmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = 0.sp,
        color = SomewhereColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = SomewhereColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = SomewhereColors.TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
        color = SomewhereColors.TextPrimary
    ),
    // Large names and card-level headings.
    titleLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = 0.sp,
        color = SomewhereColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        color = SomewhereColors.TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = SomewhereColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 23.sp,
        color = SomewhereColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 20.sp,
        color = SomewhereColors.TextPrimary
    ),
    bodySmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.35.sp,
        lineHeight = 17.sp,
        color = SomewhereColors.TextSecondary
    ),
    // Actions use tracking, but never the wordmark's exaggerated spacing.
    labelLarge = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        letterSpacing = 2.sp,
        color = SomewhereColors.TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.75.sp,
        color = SomewhereColors.TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = SomewhereSans,
        fontWeight = FontWeight.W300,
        fontSize = 11.sp,
        letterSpacing = 1.1.sp,
        color = SomewhereColors.TextMuted
    )
)
