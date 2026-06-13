package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.rememberReduceMotionEnabled

/**
 * Signature "ping" pulse animation.
 * Three concentric rings that scale out and fade — the app's visual identity moment.
 * Uses the warm GlowAccent color for a richer, more noticeable pulse.
 * Triggers once when a drop is first discovered.
 */
@Composable
fun PingPulse(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    var started by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()

    // Outer ring — largest, slowest
    val scaleAnim by animateFloatAsState(
        targetValue = if (started) 3.5f else 0.5f,
        animationSpec = tween(if (reduceMotion) 0 else 1400, easing = EaseOutCubic),
        finishedListener = { onComplete() },
        label = "pingScale"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (started) 0f else 0.5f,
        animationSpec = tween(if (reduceMotion) 0 else 1400, easing = EaseOutCubic),
        label = "pingAlpha"
    )

    // Middle ring
    val scaleAnim2 by animateFloatAsState(
        targetValue = if (started) 2.5f else 0.3f,
        animationSpec = tween(if (reduceMotion) 0 else 1100, easing = EaseOutQuart),
        label = "pingScale2"
    )
    val alphaAnim2 by animateFloatAsState(
        targetValue = if (started) 0f else 0.45f,
        animationSpec = tween(if (reduceMotion) 0 else 1100, easing = EaseOutQuart),
        label = "pingAlpha2"
    )

    // Inner ring — smallest, fastest, brightest
    val scaleAnim3 by animateFloatAsState(
        targetValue = if (started) 1.8f else 0.2f,
        animationSpec = tween(if (reduceMotion) 0 else 800, easing = EaseOutQuint),
        label = "pingScale3"
    )
    val alphaAnim3 by animateFloatAsState(
        targetValue = if (started) 0f else 0.6f,
        animationSpec = tween(if (reduceMotion) 0 else 800, easing = EaseOutQuint),
        label = "pingAlpha3"
    )

    LaunchedEffect(Unit) { started = true }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer ring — warm glow
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(scaleAnim)
                .alpha(alphaAnim)
                .background(SomewhereColors.GlowAccent.copy(alpha = 0.4f), CircleShape)
        )
        // Middle ring — brighter warm
        Box(
            modifier = Modifier
                .size(20.dp)
                .scale(scaleAnim2)
                .alpha(alphaAnim2)
                .background(SomewhereColors.GlowAccent.copy(alpha = 0.6f), CircleShape)
        )
        // Inner ring — white core
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scaleAnim3)
                .alpha(alphaAnim3)
                .background(SomewhereColors.TextPrimary, CircleShape)
        )
    }
}
