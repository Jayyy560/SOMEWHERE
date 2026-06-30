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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.somewhere.app.SomewhereApplication
import com.somewhere.app.data.remote.DropComment
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.somewhere.app.R
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.getCategoryIcon
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.data.model.Drop
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.auth

/**
 * Bottom sheet displaying the full content of a drop.
 * better typography, redesigned waveform, subtle action buttons.
 */
@Composable
fun DropDetailSheet(
    drop: Drop,
    distanceMeters: Float? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onFindSpot: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    
    var currentUserId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = com.somewhere.app.data.remote.SupabaseManager.client.auth.currentUserOrNull()?.id
    }

    var isEditing by remember { mutableStateOf(false) }
    var editDropText by remember { mutableStateOf(drop.text) }
    var showActions by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as SomewhereApplication).repository }
    
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0) }
    var comments by remember { mutableStateOf<List<DropComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    
    LaunchedEffect(drop.id) {
        isLiked = repository.isDropLikedByMe(drop.id)
        
        launch {
            repository.getDropLikesFlow(drop.id).collect { count ->
                likeCount = count
            }
        }
        
        launch {
            repository.getCommentsFlow(drop.id).collect { lst ->
                comments = lst
            }
        }
    }

    val view = LocalView.current
    var isLikeAnimating by remember { mutableStateOf(false) }
    val likeScaleAnim by animateFloatAsState(
        targetValue = if (isLikeAnimating) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        finishedListener = { isLikeAnimating = false },
        label = "likeScale"
    )

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
                    model = drop.imageUrl,
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
            
            // Drop Expiry Indicator
            if (drop.expiresAt != null) {
                val timeRemainingMs = drop.expiresAt - System.currentTimeMillis()
                if (timeRemainingMs > 0) {
                    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(timeRemainingMs)
                    val mins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(timeRemainingMs) % 60
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Expires in ${if (hours > 0) "${hours}h " else ""}${mins}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = SomewhereColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Message text
            if (isEditing) {
                OutlinedTextField(
                    value = editDropText,
                    onValueChange = { editDropText = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SomewhereColors.TextPrimary,
                        unfocusedTextColor = SomewhereColors.TextPrimary
                    )
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false; editDropText = drop.text }) {
                        Text("Cancel", color = SomewhereColors.TextSecondary)
                    }
                    TextButton(onClick = { 
                        isEditing = false
                        scope.launch { repository.updateDropText(drop.id, editDropText) }
                    }) {
                        Text("Save", color = SomewhereColors.Accent)
                    }
                }
            } else if (editDropText.isNotBlank()) {
                Text(
                    text = editDropText,
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

            // Author Name
            if (!drop.isAnonymous && !drop.authorName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarUrl = drop.authorAvatarUrl
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SomewhereColors.Card),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUrl == "default_female") {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.default_female_avatar),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (avatarUrl == "default_male" || avatarUrl == null) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.default_male_avatar),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Dropped by ${drop.authorName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SomewhereColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Audio player
            if (!drop.audioPath.isNullOrBlank()) {
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
                                    path = drop.audioPath,
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
                    val catIcon = getCategoryIcon(drop.category, drop.isAnonymous)
                    Icon(
                        imageVector = catIcon,
                        contentDescription = drop.category,
                        modifier = Modifier.size(14.dp),
                        tint = SomewhereColors.GlowAccent
                    )
                    if (distanceMeters != null) {
                        Text(
                            text = LocationUtils.formatDistance(distanceMeters),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SomewhereColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "Here",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SomewhereColors.TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (drop.expiresAt != null) {
                        val remainingMs = drop.expiresAt - System.currentTimeMillis()
                        if (remainingMs > 0) {
                            val hours = remainingMs / (1000 * 60 * 60)
                            val mins = (remainingMs / (1000 * 60)) % 60
                            val expText = if (hours > 0) "${hours}h left" else "${mins}m left"
                            Text(
                                text = expText,
                                style = MaterialTheme.typography.bodySmall,
                                color = SomewhereColors.GlowAccent
                            )
                        } else {
                            Text(
                                text = "Expired",
                                style = MaterialTheme.typography.bodySmall,
                                color = SomewhereColors.Error
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SomewhereColors.TextMuted
                        )
                        Text(
                            text = formatTimestamp(drop.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = SomewhereColors.TextSecondary
                        )
                    }
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
                        // Block
                        IconButton(
                            onClick = onBlock,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Block User",
                                modifier = Modifier.size(18.dp),
                                tint = SomewhereColors.TextMuted
                            )
                        }
                        // Report
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this drop: \"${drop.text}\" #SomewhereApp")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(18.dp),
                                tint = SomewhereColors.TextMuted
                            )
                        }
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
                        // Edit & Delete (only if owner)
                        if (drop.authorId != null && drop.authorId == currentUserId) {
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    modifier = Modifier.size(18.dp),
                                    tint = SomewhereColors.TextMuted
                                )
                            }
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
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    if (!isLiked) {
                        isLikeAnimating = true
                    }
                    scope.launch {
                        if (isLiked) {
                            repository.unlikeDrop(drop.id)
                            likeCount--
                        } else {
                            repository.likeDrop(drop)
                            likeCount++
                        }
                        isLiked = !isLiked
                    }
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else SomewhereColors.TextMuted,
                        modifier = Modifier.scale(likeScaleAnim)
                    )
                }
                Text(
                    text = "$likeCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SomewhereColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = SomewhereColors.TextMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "${comments.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SomewhereColors.TextSecondary
                )
            }
            
            // Comments Section
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .heightIn(max = 200.dp)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(comments) { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = comment.authorName ?: "Anonymous",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = SomewhereColors.TextPrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = SomewhereColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Post Comment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(onClick = {
                    if (commentText.isNotBlank()) {
                        scope.launch {
                            try {
                                repository.addComment(drop, commentText)
                                commentText = ""
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, e.message ?: "couldnt comment. try again in sometime.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = SomewhereColors.GlowAccent)
                }
            }
            
            if (drop.imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { onFindSpot(drop.imageUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = SomewhereColors.GlassBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SomewhereColors.TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Find Exact Spot", color = SomewhereColors.TextPrimary)
                    }
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
