package com.somewhere.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Time-of-day adaptive ambient color system.
 *
 * A single [AmbientColors] value is derived from the current wall-clock time and
 * smoothly interpolated across four daily phases (morning / afternoon / evening /
 * night). Only ambient surfaces read from these colors — glass backgrounds, glow
 * blobs, shadows, rims and the ping pulse. Typography and icons stay monochrome.
 *
 * V1 uses fixed time ranges. Sunrise/sunset can be layered in later using the
 * device latitude (already available from the location permission) — pure math,
 * no network.
 */
@Immutable
data class AmbientColors(
    // Card / glass surfaces
    val glassBackground: Color,
    val glassBorder: Color,
    // The two radial gradient blob colors (historically Cyan + Magenta)
    val glowPrimary: Color,
    val glowSecondary: Color,
    // Shadow tints
    val shadowAmbient: Color,
    val shadowSpot: Color,
    // Border / bezel accent
    val rimHighlight: Color,
    // PingPulse ring tint
    val pulseColor: Color,
)

/** 🌅 Morning — icy, cool, waking up. */
private val MorningAmbient = AmbientColors(
    glassBackground = Color(0xCC101828),
    glassBorder = Color(0x338EBBFF),
    glowPrimary = Color(0xFF80D8FF),
    glowSecondary = Color(0xFFB3E5FC),
    shadowAmbient = Color(0x1A64B5F6),
    shadowSpot = Color(0x2690CAF9),
    rimHighlight = Color(0xFFB3E5FC),
    pulseColor = Color(0xFFB3E5FC),
)

/** ☀️ Afternoon — clean, neutral, bright daylight. */
private val AfternoonAmbient = AmbientColors(
    glassBackground = Color(0xCC121218),
    glassBorder = Color(0x33FFFFFF),
    glowPrimary = Color(0xFFFFFFFF),
    glowSecondary = Color(0xFFE0E0E0),
    shadowAmbient = Color(0x1A000000),
    shadowSpot = Color(0x26000000),
    rimHighlight = Color(0xFFFFFFFF),
    pulseColor = Color(0xFFFFFFFF),
)

/** 🌇 Evening — warm amber, golden hour. */
private val EveningAmbient = AmbientColors(
    glassBackground = Color(0xCC1A1210),
    glassBorder = Color(0x33FFB366),
    glowPrimary = Color(0xFFFFAB40),
    glowSecondary = Color(0xFFEA80FC),
    shadowAmbient = Color(0x1AFF6D00),
    shadowSpot = Color(0x26FF00FF),
    rimHighlight = Color(0xFFFFB74D),
    pulseColor = Color(0xFFE8D5B7),
)

/** 🌙 Night — the current default look (cyan + magenta). */
private val NightAmbient = AmbientColors(
    glassBackground = Color(0xCC121218),
    glassBorder = Color(0x33FFFFFF),
    glowPrimary = Color(0xFF80D8FF),
    glowSecondary = Color(0xFFEA80FC),
    shadowAmbient = Color(0x1A00E5FF),
    shadowSpot = Color(0x26FF00FF),
    rimHighlight = Color(0xFF80D8FF),
    pulseColor = Color(0xFFE8D5B7),
)

/** Interpolate between two palettes field-by-field. [fraction] 0f → [a], 1f → [b]. */
fun lerp(a: AmbientColors, b: AmbientColors, fraction: Float): AmbientColors {
    val f = fraction.coerceIn(0f, 1f)
    return AmbientColors(
        glassBackground = lerp(a.glassBackground, b.glassBackground, f),
        glassBorder = lerp(a.glassBorder, b.glassBorder, f),
        glowPrimary = lerp(a.glowPrimary, b.glowPrimary, f),
        glowSecondary = lerp(a.glowSecondary, b.glowSecondary, f),
        shadowAmbient = lerp(a.shadowAmbient, b.shadowAmbient, f),
        shadowSpot = lerp(a.shadowSpot, b.shadowSpot, f),
        rimHighlight = lerp(a.rimHighlight, b.rimHighlight, f),
        pulseColor = lerp(a.pulseColor, b.pulseColor, f),
    )
}

// Phase boundaries expressed in minutes-from-midnight. Each 30-minute transition
// zone linearly interpolates from the outgoing palette to the incoming one.
//
//  [00:00 .. 05:00) Night
//  [05:00 .. 05:30) Night     → Morning
//  [05:30 .. 09:30) Morning
//  [09:30 .. 10:00) Morning   → Afternoon
//  [10:00 .. 16:00) Afternoon
//  [16:00 .. 16:30) Afternoon → Evening
//  [16:30 .. 19:00) Evening
//  [19:00 .. 19:30) Evening   → Night
//  [19:30 .. 24:00) Night
private const val TRANSITION_MINUTES = 30

private const val MORNING_START = 5 * 60 + 30      // 05:30
private const val AFTERNOON_START = 10 * 60         // 10:00
private const val EVENING_START = 16 * 60 + 30      // 16:30
private const val NIGHT_START = 19 * 60 + 30        // 19:30

/**
 * Resolve the interpolated ambient palette for a given [minuteOfDay] (0..1439).
 * Pure function — no clock access — so it's trivially testable.
 */
fun ambientColorsForMinute(minuteOfDay: Int): AmbientColors {
    val m = ((minuteOfDay % 1440) + 1440) % 1440

    // A transition zone is the 30 minutes ending at a phase's start boundary.
    fun transition(startBoundary: Int, from: AmbientColors, to: AmbientColors): AmbientColors? {
        val zoneStart = startBoundary - TRANSITION_MINUTES
        return if (m in zoneStart until startBoundary) {
            lerp(from, to, (m - zoneStart).toFloat() / TRANSITION_MINUTES)
        } else null
    }

    transition(MORNING_START, NightAmbient, MorningAmbient)?.let { return it }
    transition(AFTERNOON_START, MorningAmbient, AfternoonAmbient)?.let { return it }
    transition(EVENING_START, AfternoonAmbient, EveningAmbient)?.let { return it }
    transition(NIGHT_START, EveningAmbient, NightAmbient)?.let { return it }

    // Outside any transition zone → a steady palette.
    return when {
        m < MORNING_START -> NightAmbient        // 00:00–05:00 (05:00–05:30 handled above)
        m < AFTERNOON_START -> MorningAmbient     // 05:30–09:30
        m < EVENING_START -> AfternoonAmbient     // 10:00–16:00
        m < NIGHT_START -> EveningAmbient          // 16:30–19:00
        else -> NightAmbient                       // 19:30–24:00
    }
}

private fun currentMinuteOfDay(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/**
 * Recomputes the ambient palette every 60 seconds from the wall clock.
 * Provide the result via [LocalAmbientColors] at the top of the tree.
 */
@Composable
fun rememberAmbientColors(): AmbientColors {
    val colors by produceState(initialValue = ambientColorsForMinute(currentMinuteOfDay())) {
        while (true) {
            value = ambientColorsForMinute(currentMinuteOfDay())
            delay(60_000L)
        }
    }
    return colors
}

/**
 * Ambient colors for the current time of day. Defaults to the night palette so
 * previews and any composable read outside the provider stay on the current look.
 */
val LocalAmbientColors = staticCompositionLocalOf { NightAmbient }
