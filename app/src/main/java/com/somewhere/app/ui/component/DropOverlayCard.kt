package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.getCategoryIcon
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DiscoveredDrop

/**
 * Floating overlay card shown on the camera preview.
 *
 * Two visual states:
 * - LOCKED (far): frosted glass, lock icon, "walk closer" with distance
 * - UNLOCKED (near): sharp glassmorphism, pin icon, text preview, warm glow
 *
 * Primary items get full opacity; secondary items are slightly faded.
 */
@Composable
fun DropOverlayCard(
    item: DiscoveredDrop,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotionEnabled()

    // Spring entrance animation
    var visible by remember { mutableStateOf(false) }
    val scaleIn by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = if (reduceMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScaleIn"
    )
    val alphaIn by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 500, easing = EaseOutCubic),
        label = "cardAlphaIn"
    )
    LaunchedEffect(Unit) { visible = true }

    // Bounce on unlock
    val view = androidx.compose.ui.platform.LocalView.current
    var unlockBounce by remember { mutableStateOf(false) }
    val unlockScaleAnim by animateFloatAsState(
        targetValue = if (unlockBounce) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        finishedListener = { unlockBounce = false },
        label = "unlockBounce"
    )

    LaunchedEffect(item.isUnlocked) {
        if (item.isUnlocked) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            unlockBounce = true
        }
    }

    // Primary vs secondary sizing
    val cardScale = if (item.isPrimary) 1f else 0.85f
    val baseAlpha = if (item.isPrimary) 1f else 0.75f

    // Locked shimmer — subtle breathing animation
    val shimmerAlpha by if (!item.isUnlocked) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmerAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    // Unlocked glow pulse
    val glowAlpha by if (item.isUnlocked) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowPulse"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .scale(cardScale * scaleIn * unlockScaleAnim)
            .alpha(alphaIn * baseAlpha * shimmerAlpha),
        contentAlignment = Alignment.Center
    ) {

        // Wavy animated shape
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val waveProgress by infiniteTransition.animateFloat(
            initialValue = 0f, 
            targetValue = (2 * kotlin.math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing), 
                repeatMode = RepeatMode.Restart
            ),
            label = "wave"
        )
        val bubblyShape = WavyPillShape(waveProgress, 1.2f)

        // Gentle breathing animation for the entire card to make left/right sides look alive
        val scaleX by infiniteTransition.animateFloat(
            initialValue = 0.98f, targetValue = 1.02f,
            animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breatheX"
        )
        val scaleY by infiniteTransition.animateFloat(
            initialValue = 1.02f, targetValue = 0.98f,
            animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breatheY"
        )

        // Main card body
        Column(
            modifier = Modifier
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                }
                .then(
                    if (item.isUnlocked) {
                        Modifier.clickable(onClick = onTap)
                    } else Modifier
                )
                .then(
                    if (!item.isUnlocked) Modifier.blur(3.dp) else Modifier
                )
                .shadow(
                    elevation = if (item.isUnlocked) 16.dp else 4.dp,
                    shape = bubblyShape,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(bubblyShape)
                // Base tint for contrast
                .background(Color.Black.copy(alpha = 0.3f))
                // Flat whitish frost (removes stain effect from gradients)
                .background(Color.White.copy(alpha = 0.15f))
                // Thick 3D inner bezel
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.6f)
                        )
                    ),
                    shape = bubblyShape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .widthIn(min = 80.dp, max = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (item.isUnlocked) {
                // ── Unlocked state ──

                // Distance pill with category icon
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SomewhereColors.DistancePill)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val icon = getCategoryIcon(item.drop.category, item.drop.isAnonymous)
                    Icon(
                        imageVector = icon,
                        contentDescription = item.drop.category,
                        modifier = Modifier.size(12.dp),
                        tint = SomewhereColors.GlowAccent
                    )
                    Text(
                        text = LocationUtils.formatDistance(item.distanceMeters),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SomewhereColors.DistancePillText
                    )
                }

                if (item.drop.expiresAt != null) {
                    val remainingMs = item.drop.expiresAt - System.currentTimeMillis()
                    if (remainingMs > 0) {
                        val hours = remainingMs / (1000 * 60 * 60)
                        val mins = (remainingMs / (1000 * 60)) % 60
                        val expText = if (hours > 0) "Expires in ${hours}h" else "Expires in ${mins}m"
                        Text(
                            text = expText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SomewhereColors.GlowAccent
                        )
                    }
                }

                // Text preview
                if (item.drop.text.isNotBlank()) {
                    Text(
                        text = item.drop.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        ),
                        color = SomewhereColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // ── Locked state ──

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(20.dp),
                    tint = SomewhereColors.LockTint
                )

                Text(
                    text = "walk closer",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = SomewhereColors.LockTint
                )

                // Show how far they need to walk
                Text(
                    text = "${item.distanceMeters.toInt()}m away",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = SomewhereColors.TextMuted
                )
            }
        }
    }
}
