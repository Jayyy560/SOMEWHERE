package com.somewhere.app.ui.component

import android.net.Uri
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AccessTime
import com.somewhere.app.SomewhereApplication
import com.somewhere.app.data.remote.DropComment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * Minimalist, monochromatic, sharp styling perfectly aligned with Somewhere theme.
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
    var isDownloading by remember { mutableStateOf(false) }
    
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
        targetValue = if (expanded) 1f else 0.95f,
        animationSpec = if (reduceMotion) tween(0) else tween(200, easing = LinearOutSlowInEasing),
        label = "detailScale"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 200),
        label = "detailAlpha"
    )

    LaunchedEffect(Unit) { expanded = true }
    BackHandler(onBack = onDismiss)

    // Dimmed flat backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
            .background(SomewhereColors.Background.copy(alpha = 0.95f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Main minimal panel
        Column(
            modifier = Modifier
                .offset { IntOffset(0, dragOffset.roundToInt()) }
                .scale(scaleAnim)
                .alpha(alphaAnim)
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .padding(horizontal = 16.dp)
                .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Drag Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(SomewhereColors.CardBorder)
                )
            }

            // Hero Image
            if (drop.imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                ) {
                    AsyncImage(
                        model = drop.imageUrl,
                        contentDescription = "Drop photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )

                    // Sharp close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .background(SomewhereColors.Background, RoundedCornerShape(2.dp))
                            .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(14.dp),
                            tint = SomewhereColors.TextPrimary
                        )
                    }

                    // Expiry tag
                    if (drop.expiresAt != null) {
                        val timeRemainingMs = drop.expiresAt - System.currentTimeMillis()
                        if (timeRemainingMs > 0) {
                            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(timeRemainingMs)
                            val mins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(timeRemainingMs) % 60
                            val expiryText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(SomewhereColors.Background, RoundedCornerShape(2.dp))
                                    .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = expiryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SomewhereColors.TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Header Meta (Avatar & Name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!drop.isAnonymous && !drop.authorName.isNullOrBlank()) {
                    val avatarUrl = drop.authorAvatarUrl
                    val modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SomewhereColors.CardBorder)
                        
                    if (avatarUrl == "default_female") {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.default_female_avatar),
                            contentDescription = "Avatar",
                            modifier = modifier,
                            contentScale = ContentScale.Crop
                        )
                    } else if (avatarUrl == "default_male" || avatarUrl == null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.default_male_avatar),
                            contentDescription = "Avatar",
                            modifier = modifier,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = modifier,
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = drop.authorName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = SomewhereColors.TextPrimary
                    )
                } else {
                    Text(
                        text = "ANONYMOUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = SomewhereColors.TextMuted
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                Text(
                    text = formatTimestamp(drop.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = SomewhereColors.TextMuted
                )
            }
            
            Spacer(Modifier.height(16.dp))

            // Body Text
            if (isEditing) {
                OutlinedTextField(
                    value = editDropText,
                    onValueChange = { editDropText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SomewhereColors.TextPrimary,
                        unfocusedTextColor = SomewhereColors.TextPrimary,
                        focusedBorderColor = SomewhereColors.TextPrimary,
                        unfocusedBorderColor = SomewhereColors.CardBorder
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditing = false; editDropText = drop.text }) {
                        Text("CANCEL", color = SomewhereColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = {
                        isEditing = false
                        scope.launch { repository.updateDropText(drop.id, editDropText) }
                    }) {
                        Text("SAVE", color = SomewhereColors.TextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (editDropText.isNotBlank()) {
                Text(
                    text = editDropText,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = SomewhereColors.TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Meta Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetaLabel(
                    icon = getCategoryIcon(drop.category, drop.isAnonymous),
                    text = drop.category?.uppercase() ?: "STORY"
                )
                if (distanceMeters != null) {
                    MetaLabel(
                        icon = Icons.Default.LocationOn,
                        text = LocationUtils.formatDistance(distanceMeters).uppercase()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Audio Player (Sharp and Minimal)
            if (!drop.audioPath.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (player?.isPlaying == true) {
                                player?.pause()
                                isAudioPlaying = false
                            } else if (player != null) {
                                runCatching { player?.start(); isAudioPlaying = true }
                            } else {
                                player = playAudio(context, drop.audioPath) {
                                    isAudioPlaying = false
                                    player = null
                                }
                                isAudioPlaying = player != null
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                    ) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAudioPlaying) "Pause" else "Play",
                            tint = SomewhereColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DetailWaveform(active = isAudioPlaying, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Dead Drop Card (Brutalist style)
            if (drop.isDeadDrop && drop.fileUrl != null) {
                val canDownload = distanceMeters != null && distanceMeters <= 15f
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "File",
                        tint = SomewhereColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = drop.fileName ?: "FILE",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SomewhereColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sizeMb = (drop.fileSize ?: 0L) / (1024f * 1024f)
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f MB", sizeMb),
                            style = MaterialTheme.typography.labelSmall,
                            color = SomewhereColors.TextMuted
                        )
                    }

                    if (canDownload) {
                        IconButton(
                            onClick = {
                                if (!isDownloading) {
                                    isDownloading = true
                                    scope.launch {
                                        try {
                                            val file = repository.downloadDeadDropFile(
                                                drop.fileUrl,
                                                drop.fileName ?: "drop_${drop.id}"
                                            )
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, drop.fileType ?: "*/*")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Open File"))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isDownloading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = SomewhereColors.TextPrimary,
                                    strokeWidth = 1.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = SomewhereColors.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = SomewhereColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (!canDownload) {
                    val currentDistance = distanceMeters?.toInt() ?: "?"
                    Text(
                        text = "MOVE CLOSER TO UNLOCK ($currentDistance M AWAY)",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SomewhereColors.TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SomewhereColors.CardBorder)
            )

            // Social Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (!isLiked) isLikeAnimating = true
                        scope.launch {
                            if (isLiked) { repository.unlikeDrop(drop.id); likeCount-- } 
                            else { repository.likeDrop(drop); likeCount++ }
                            isLiked = !isLiked
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) SomewhereColors.TextPrimary else SomewhereColors.TextMuted,
                        modifier = Modifier.size(20.dp).scale(likeScaleAnim)
                    )
                }
                Text(
                    text = "$likeCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = SomewhereColors.TextSecondary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = SomewhereColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${comments.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SomewhereColors.TextSecondary
                )

                Spacer(modifier = Modifier.weight(1f))

                if (showActions) {
                    IconButton(onClick = { /* Share intent */ }) {
                        Icon(Icons.Default.Share, "Share", Modifier.size(18.dp), SomewhereColors.TextMuted)
                    }
                    IconButton(onClick = onBlock) {
                        Icon(Icons.Default.Block, "Block", Modifier.size(18.dp), SomewhereColors.TextMuted)
                    }
                    IconButton(onClick = onReport) {
                        Icon(Icons.Default.Report, "Report", Modifier.size(18.dp), SomewhereColors.TextMuted)
                    }
                    if (drop.authorId == currentUserId) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "Edit", Modifier.size(18.dp), SomewhereColors.TextMuted)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", Modifier.size(18.dp), SomewhereColors.TextMuted)
                        }
                    }
                }
                IconButton(onClick = { showActions = !showActions }) {
                    Icon(Icons.Default.MoreHoriz, "More", Modifier.size(18.dp), SomewhereColors.TextMuted)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SomewhereColors.CardBorder)
            )

            // Comments List (Sharp lines, left bordered)
            if (comments.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    comments.takeLast(5).forEach { comment ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(32.dp)
                                    .background(SomewhereColors.CardBorder)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = (comment.authorName ?: "ANON").uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SomewhereColors.TextMuted
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SomewhereColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Comment Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Comment...", color = SomewhereColors.TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    shape = RoundedCornerShape(2.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SomewhereColors.TextPrimary,
                        unfocusedBorderColor = SomewhereColors.CardBorder,
                        focusedContainerColor = SomewhereColors.Background,
                        unfocusedContainerColor = SomewhereColors.Background
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            scope.launch {
                                runCatching {
                                    repository.addComment(drop, commentText)
                                    commentText = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (commentText.isNotBlank()) SomewhereColors.TextPrimary else SomewhereColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Find Exact Spot button (if image exists)
            if (drop.imageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 24.dp)
                        .border(1.dp, SomewhereColors.CardBorder, RoundedCornerShape(2.dp))
                        .clickable { onFindSpot(drop.imageUrl) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = SomewhereColors.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "FIND EXACT SPOT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = SomewhereColors.TextPrimary
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player?.stop(); player?.release() }
            isAudioPlaying = false
        }
    }
}

@Composable
private fun MetaLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = SomewhereColors.TextMuted)
        Text(text, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = SomewhereColors.TextMuted)
    }
}

@Composable
private fun DetailWaveform(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "detailWave")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Restart),
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
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height((4 + heightScale * 20f).dp)
                    .background(if (active) SomewhereColors.TextPrimary else SomewhereColors.TextMuted)
            )
        }
    }
}

private fun playAudio(context: android.content.Context, path: String, onComplete: () -> Unit): MediaPlayer? {
    return runCatching {
        MediaPlayer().apply {
            if (path.startsWith("/")) setDataSource(path)
            else setDataSource(context, Uri.parse(path))
            prepare(); start()
            setOnCompletionListener { mp -> onComplete(); mp.release() }
        }
    }.getOrNull()
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "JUST NOW"
        diff < 3_600_000 -> "${diff / 60_000}M AGO"
        diff < 86_400_000 -> "${diff / 3_600_000}H AGO"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp)).uppercase()
    }
}
