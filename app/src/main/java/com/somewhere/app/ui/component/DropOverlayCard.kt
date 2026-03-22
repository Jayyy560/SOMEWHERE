package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DiscoveredDrop

/**
 * Floating overlay card shown on the camera preview.
 *
 * Two visual states:
 * - LOCKED (far): blurred, faint, shows "move closer", non-tappable
 * - UNLOCKED (near): sharp, shows distance, tappable
 *
 * Primary items get full opacity; secondary items are slightly faded.
 */
@Composable
fun DropOverlayCard(
    item: DiscoveredDrop,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fade-in animation when card first appears
    var visible by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()
    val alpha by animateFloatAsState(
        targetValue = if (visible) {
            if (item.isPrimary) 1f else 0.6f
        } else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 600, easing = EaseOutCubic),
        label = "cardAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    // Subtle scale for primary vs secondary
    val cardScale = if (item.isPrimary) 1f else 0.88f

    // Locked shimmer — subtle opacity oscillation to feel alive
    val shimmerAlpha by if (!item.isUnlocked) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmerAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .scale(cardScale)
            .alpha(alpha * shimmerAlpha)
            .then(
                if (item.isUnlocked) {
                    Modifier.clickable(onClick = onTap)
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (!item.isUnlocked) Modifier.blur(4.dp) else Modifier
                )
                .clip(RoundedCornerShape(4.dp))
                .background(SomewhereColors.Card.copy(alpha = 0.85f))
                .border(0.5.dp, SomewhereColors.CardBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(min = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (item.isUnlocked) {
                    LocationUtils.formatDistance(item.distanceMeters)
                } else {
                    "move closer"
                },
                style = if (item.isUnlocked) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.labelSmall
                }
            )

            if (item.isUnlocked) {
                Text(
                    text = item.drop.text.take(30) + if (item.drop.text.length > 30) "…" else "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }
    }
}
