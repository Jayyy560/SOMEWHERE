package com.somewhere.app.ui.component

import android.net.Uri
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DiscoveredDrop
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Expanded detail view for a discovered drop.
 * Animates in from overlay → center with dimmed backdrop.
 * Premium design: rounded image with gradient overlay, drag handle,
 * better typography, redesigned waveform, subtle action buttons.
 */
@Composable
fun DropDetailSheet(
    item: DiscoveredDrop,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.85f,
        animationSpec = if (reduceMotion) {
            tween(0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "detailScale"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 400, easing = EaseOutCubic),
        label = "detailAlpha"
    )

    LaunchedEffect(Unit) { expanded = true }

    BackHandler(onBack = onDismiss)

    // Dimmed backdrop with vignette
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SomewhereColors.Overlay,
                        Color(0xF00A0A0A) // Darker at edges for vignette
                    ),
                    radius = 1200f
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Detail card
        Column(
            modifier = Modifier
                .offset { IntOffset(0, dragOffset.roundToInt()) }
                .scale(scaleAnim)
                .alpha(alphaAnim)
                .widthIn(max = 340.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = SomewhereColors.GlowAccent.copy(alpha = 0.1f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(SomewhereColors.Surface)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click */ }
                .draggable(
                    state = rememberDraggableState { delta ->
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (dragOffset > 140f) {
                            onDismiss()
                        } else {
                            scope.launch { dragOffset = 0f }
                        }
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SomewhereColors.TextMuted)
            )

            // Photo with gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                val context = LocalContext.current
                AsyncImage(
                    model = item.drop.imageUrl,
                    contentDescription = "Drop photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    contentScale = ContentScale.Crop
                )

                // Bottom gradient for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )

                // Close button on image
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(
                            SomewhereColors.Background.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = SomewhereColors.TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Message text
            if (item.drop.text.isNotBlank()) {
                Text(
                    text = item.drop.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    ),
                    color = SomewhereColors.TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(12.dp))
            }

            // Audio player
            if (!item.drop.audioPath.isNullOrBlank()) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SomewhereColors.Card)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play/pause button
                    IconButton(
                        onClick = {
                            if (player?.isPlaying == true) {
                                player?.pause()
                                isAudioPlaying = false
                            } else if (player != null) {
                                runCatching {
                                    player?.start()
                                    isAudioPlaying = true
                                }
                            } else {
                                player = playAudio(
                                    context = context,
                                    path = item.drop.audioPath,
                                    onComplete = {
                                        isAudioPlaying = false
                                        player = null
                                    }
                                )
                                isAudioPlaying = player != null
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                SomewhereColors.GlowAccent.copy(alpha = 0.15f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isAudioPlaying) "Pause" else "Play",
                            tint = SomewhereColors.GlowAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Waveform
                    DetailWaveform(
                        active = isAudioPlaying,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(0.5.dp)
                    .background(SomewhereColors.Divider)
            )

            Spacer(Modifier.height(12.dp))

            // Metadata row: distance + timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SomewhereColors.GlowAccent
                    )
                    Text(
                        text = LocationUtils.formatDistance(item.distanceMeters),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SomewhereColors.TextSecondary
                    )
                }

                // Timestamp with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = SomewhereColors.TextMuted
                    )
                    Text(
                        text = formatTimestamp(item.drop.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = SomewhereColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action row — subtle overflow style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showActions) {
                    // Expanded actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Report
                        IconButton(
                            onClick = onReport,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = "Report",
                                modifier = Modifier.size(18.dp),
                                tint = SomewhereColors.TextMuted
                            )
                        }
                        // Delete
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(18.dp),
                                tint = SomewhereColors.Error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // More button
                IconButton(
                    onClick = { showActions = !showActions },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        modifier = Modifier.size(18.dp),
                        tint = SomewhereColors.TextMuted
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                player?.stop()
                player?.release()
            }
            isAudioPlaying = false
        }
    }
}

@Composable
private fun DetailWaveform(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "detailWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(20) { index ->
            val dynamic = abs(sin((index / 3.5f + phase) * Math.PI)).toFloat()
            val heightScale = if (active) (0.2f + dynamic * 0.8f) else 0.12f
            // Gradient coloring: center bars are warmer
            val barAlpha = if (active) {
                0.5f + dynamic * 0.5f
            } else {
                0.3f
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height((4 + heightScale * 20f).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (active) SomewhereColors.GlowAccent.copy(alpha = barAlpha)
                        else SomewhereColors.TextMuted.copy(alpha = barAlpha)
                    )
            )
        }
    }
}

private fun playAudio(
    context: android.content.Context,
    path: String,
    onComplete: () -> Unit
): MediaPlayer? {
    return runCatching {
        MediaPlayer().apply {
            if (path.startsWith("/")) {
                setDataSource(path)
            } else {
                setDataSource(context, Uri.parse(path))
            }
            prepare()
            start()
            setOnCompletionListener { mp ->
                onComplete()
                mp.release()
            }
        }
    }.getOrNull()
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
