package com.somewhere.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.theme.SomewhereColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.somewhere.app.util.rememberReduceMotionEnabled

/**
 * Home screen — minimal entry point.
 * "SOMEWHERE" title + two action buttons.
 * Lots of negative space, calm atmosphere.
 */
@Composable
fun HomeScreen(
    onExplore: () -> Unit,
    onDrop: () -> Unit,
    onSettings: () -> Unit
) {
    // Subtle entrance fade
    var visible by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()
    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 1200, easing = EaseOutCubic),
        label = "titleFade"
    )
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 800, delayMillis = if (reduceMotion) 0 else 500, easing = EaseOutCubic),
        label = "buttonsFade"
    )
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
            .systemBarsPadding()
    ) {
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }

        // Title — positioned slightly above center
        Text(
            text = "SOMEWHERE",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .alpha(titleAlpha)
        )

        // Action buttons — lower portion
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .alpha(buttonsAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SomewhereButton(
                text = "Explore",
                onClick = onExplore
            )
            SomewhereButton(
                text = "Leave something here",
                onClick = onDrop
            )
        }
    }
}
