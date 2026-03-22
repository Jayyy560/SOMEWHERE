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
 * Concentric rings that scale out and fade — the app's visual identity moment.
 * Triggers once when a drop is first discovered.
 */
@Composable
fun PingPulse(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    var started by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()

    val scaleAnim by animateFloatAsState(
        targetValue = if (started) 3f else 0.5f,
        animationSpec = tween(if (reduceMotion) 0 else 1200, easing = EaseOutCubic),
        finishedListener = { onComplete() },
        label = "pingScale"
    )

    val alphaAnim by animateFloatAsState(
        targetValue = if (started) 0f else 0.6f,
        animationSpec = tween(if (reduceMotion) 0 else 1200, easing = EaseOutCubic),
        label = "pingAlpha"
    )

    // Second ring, slightly delayed feel via different easing
    val scaleAnim2 by animateFloatAsState(
        targetValue = if (started) 2.2f else 0.3f,
        animationSpec = tween(if (reduceMotion) 0 else 1000, easing = EaseOutQuart),
        label = "pingScale2"
    )

    val alphaAnim2 by animateFloatAsState(
        targetValue = if (started) 0f else 0.4f,
        animationSpec = tween(if (reduceMotion) 0 else 1000, easing = EaseOutQuart),
        label = "pingAlpha2"
    )

    LaunchedEffect(Unit) { started = true }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scaleAnim)
                .alpha(alphaAnim)
                .background(SomewhereColors.AccentSubtle, CircleShape)
        )
        // Inner ring
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(scaleAnim2)
                .alpha(alphaAnim2)
                .background(SomewhereColors.TextPrimary, CircleShape)
        )
    }
}
