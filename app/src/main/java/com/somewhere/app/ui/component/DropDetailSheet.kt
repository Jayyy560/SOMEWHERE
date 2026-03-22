package com.somewhere.app.ui.component

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Report
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.DiscoveredDrop
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Expanded detail view for a discovered drop.
 * Animates in from overlay → center with dimmed backdrop.
 * Shows photo, message text, distance, and timestamp.
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

    // Dimmed backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
            .background(SomewhereColors.Overlay)
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
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(6.dp))
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
                )
                .padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
            // Photo
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(item.drop.imagePath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Drop photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(20.dp))

            // Message text
            Text(
                text = item.drop.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Distance + timestamp row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = LocationUtils.formatDistance(item.distanceMeters),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatTimestamp(item.drop.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onReport) {
                        Icon(imageVector = Icons.Default.Report, contentDescription = "Report")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
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
