package com.somewhere.app.ui.component

import android.net.Uri
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn

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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Download
import com.somewhere.app.SomewhereApplication
import com.somewhere.app.data.remote.DropComment
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.data.model.Drop
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.auth

// Glass panel: translucent dark with blue tint — lets the image bleed through
private val GlassPanelBg = Color(0xCC1E1E28)
private val InnerBg = Color(0xFF2A2A32)
private val TextHi = Color(0xFFF0F0F0)
private val TextMd = Color(0xFF9A9AA0)
private val TextLo = Color(0xFF55555A)

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
    var isHoldingPhoto by remember { mutableStateOf(false) }
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
        launch { repository.getDropLikesFlow(drop.id).collect { likeCount = it } }
        launch { repository.getCommentsFlow(drop.id).collect { comments = it } }
    }

    val view = LocalView.current
    var isLikeAnimating by remember { mutableStateOf(false) }
    val likeScaleAnim by animateFloatAsState(
        targetValue = if (isLikeAnimating) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        finishedListener = { isLikeAnimating = false },
        label = "ls"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.9f,
        animationSpec = if (reduceMotion) tween(0)
        else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "s"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 300),
        label = "a"
    )

    LaunchedEffect(Unit) { expanded = true }
    BackHandler(onBack = onDismiss)

    // Backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
            .background(Color(0xB3000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
    // ── Card and Hitchhiker Buttons Container ──
    Column(
        modifier = Modifier
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .scale(scaleAnim)
            .alpha(alphaAnim)
            .fillMaxWidth(0.88f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
            .draggable(
                state = rememberDraggableState { delta ->
                    dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (dragOffset > 100f) onDismiss()
                    else scope.launch { dragOffset = 0f }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            // Layer 1: Full image
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isHoldingPhoto = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    isHoldingPhoto = false
                                }
                            }
                        )
                    }
            ) {
                AsyncImage(
                    model = drop.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                    contentScale = ContentScale.Crop
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isHoldingPhoto,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.matchParentSize()
            ) {
                Box(Modifier.fillMaxSize()) {

            // Layer 2: Close button (top right)
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp)
                    .clip(CircleShape).background(Color(0x55000000))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Close, null, Modifier.size(16.dp), Color.White) }

            // Expiry badge (moment drops only)
            if (drop.expiresAt != null) {
                val r = drop.expiresAt - System.currentTimeMillis()
                if (r > 0) {
                    val h = r / 3_600_000; val m = (r / 60_000) % 60
                    Text(
                        if (h > 0) "${h}h ${m}m left" else "${m}m left",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            .background(Color(0x55000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Layer 3: Glass panel — overlaps the bottom of the image
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(GlassPanelBg)
            ) {
                // Author + text + meta
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!drop.isAnonymous && !drop.authorName.isNullOrBlank()) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(InnerBg),
                                contentAlignment = Alignment.Center
                            ) {
                                val av = drop.authorAvatarUrl
                                when {
                                    av == "default_female" -> androidx.compose.foundation.Image(
                                        painterResource(R.drawable.default_female_avatar), null,
                                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    av == "default_male" || av == null -> androidx.compose.foundation.Image(
                                        painterResource(R.drawable.default_male_avatar), null,
                                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    else -> AsyncImage(av, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(drop.authorName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextHi)
                        } else {
                            Text("Anonymous", style = MaterialTheme.typography.bodyMedium, color = TextMd)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            buildString {
                                append(if (distanceMeters != null) LocationUtils.formatDistance(distanceMeters) else "here")
                                append(" · ")
                                if (drop.expiresAt != null) {
                                    val r2 = drop.expiresAt - System.currentTimeMillis()
                                    if (r2 > 0) { val hh = r2/3_600_000; val mm = (r2/60_000)%60; append(if (hh>0) "${hh}h" else "${mm}m") }
                                    else append("expired")
                                } else append(formatTimestamp(drop.timestamp))
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMd
                        )
                    }

                    if (!isEditing && editDropText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            editDropText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
                            color = TextHi,
                            maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Edit mode
                if (isEditing) {
                    TextField(
                        value = editDropText, onValueChange = { editDropText = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextHi, unfocusedTextColor = TextHi,
                            focusedContainerColor = InnerBg, unfocusedContainerColor = InnerBg,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ), shape = RoundedCornerShape(12.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isEditing = false; editDropText = drop.text }) { Text("Cancel", color = TextMd) }
                        TextButton(onClick = {
                            isEditing = false; scope.launch { repository.updateDropText(drop.id, editDropText) }
                        }) { Text("Save", color = TextHi) }
                    }
                }

                // Audio
                if (!drop.audioPath.isNullOrBlank()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp)).background(InnerBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            if (player?.isPlaying == true) { player?.pause(); isAudioPlaying = false }
                            else if (player != null) { runCatching { player?.start(); isAudioPlaying = true } }
                            else {
                                player = playAudio(context, drop.audioPath) { isAudioPlaying = false; player = null }
                                isAudioPlaying = player != null
                            }
                        }, Modifier.size(32.dp)) {
                            Icon(if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, Modifier.size(18.dp), TextHi)
                        }
                        DetailWaveform(active = isAudioPlaying, modifier = Modifier.weight(1f))
                    }
                }

                // Dead Drop File
                if (drop.isDeadDrop && drop.fileUrl != null) {
                    val canDownload = distanceMeters != null && distanceMeters <= 15f
                    val fileIcon = when {
                        drop.fileType?.startsWith("image/") == true -> Icons.Default.Image
                        drop.fileType?.startsWith("audio/") == true -> Icons.Default.Audiotrack
                        drop.fileType?.startsWith("video/") == true -> Icons.Default.Movie
                        drop.fileType?.startsWith("model/") == true -> Icons.Default.ViewInAr
                        else -> Icons.Default.Description
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp)).background(InnerBg).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(fileIcon, null, Modifier.size(20.dp), TextMd)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(drop.fileName ?: "Attachment",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = TextHi, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val mb = (drop.fileSize ?: 0L) / (1024f * 1024f)
                            Text(String.format(java.util.Locale.US, "%.1f MB", mb),
                                style = MaterialTheme.typography.labelSmall, color = TextLo)
                        }
                        if (canDownload) {
                            IconButton(onClick = {
                                if (!isDownloading) {
                                    isDownloading = true
                                    scope.launch {
                                        try {
                                            val file = repository.downloadDeadDropFile(drop.fileUrl, drop.fileName ?: "drop_${drop.id}")
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, drop.fileType ?: "*/*"); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Open"))
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
                                        } finally { isDownloading = false }
                                    }
                                }
                            }, Modifier.size(32.dp)) {
                                if (isDownloading) CircularProgressIndicator(Modifier.size(16.dp), TextMd, strokeWidth = 1.5.dp)
                                else Icon(Icons.Default.Download, null, Modifier.size(18.dp), TextHi)
                            }
                        } else {
                            Icon(Icons.Default.Lock, null, Modifier.size(18.dp), TextLo)
                        }
                    }
                    if (!canDownload) {
                        Text("${distanceMeters?.toInt() ?: "?"}m away · walk within 15m",
                            style = MaterialTheme.typography.labelSmall, color = TextLo,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
                    }
                }

                // Social row
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (!isLiked) isLikeAnimating = true
                        scope.launch {
                            if (isLiked) { repository.unlikeDrop(drop.id); likeCount-- }
                            else { repository.likeDrop(drop); likeCount++ }
                            isLiked = !isLiked
                        }
                    }, Modifier.size(34.dp)) {
                        Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null, Modifier.size(18.dp).scale(likeScaleAnim),
                            if (isLiked) Color(0xFFFF453A) else TextMd)
                    }
                    Text("$likeCount", style = MaterialTheme.typography.labelMedium, color = TextMd)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(16.dp), TextMd)
                    Spacer(Modifier.width(4.dp))
                    Text("${comments.size}", style = MaterialTheme.typography.labelMedium, color = TextMd)

                    Spacer(Modifier.weight(1f))

                    // Find Spot — clearly visible
                    if (drop.imageUrl != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(InnerBg)
                                .clickable { onFindSpot(drop.imageUrl) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, Modifier.size(14.dp), TextMd)
                            Text("Find Spot",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextMd)
                        }
                    }

                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { showActions = !showActions }, Modifier.size(30.dp)) {
                        Icon(Icons.Default.MoreHoriz, null, Modifier.size(18.dp), TextMd)
                    }
                }

                // Overflow
                if (showActions) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = {
                            val i = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "\"${drop.text}\""); type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(i, null))
                        }, Modifier.size(30.dp)) { Icon(Icons.Default.Share, null, Modifier.size(16.dp), TextMd) }
                        IconButton(onClick = onBlock, Modifier.size(30.dp)) { Icon(Icons.Default.Block, null, Modifier.size(16.dp), TextMd) }
                        IconButton(onClick = onReport, Modifier.size(30.dp)) { Icon(Icons.Default.Report, null, Modifier.size(16.dp), TextMd) }
                        if (drop.authorId != null && drop.authorId == currentUserId) {
                            IconButton(onClick = { isEditing = true }, Modifier.size(30.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp), TextMd) }
                            IconButton(onClick = onDelete, Modifier.size(30.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), Color(0xFFFF453A)) }
                        }
                    }
                }

                // Comments
                if (comments.isNotEmpty()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        comments.takeLast(4).forEach { c ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(c.authorName ?: "anon",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextHi)
                                Spacer(Modifier.width(6.dp))
                                Text(c.text, style = MaterialTheme.typography.bodySmall, color = TextMd,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Comment input
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = commentText, onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", style = MaterialTheme.typography.bodySmall, color = TextLo) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextHi),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = InnerBg, unfocusedContainerColor = InnerBg,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = TextHi
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (commentText.isNotBlank()) {
                            scope.launch {
                                try { repository.addComment(drop, commentText); commentText = "" }
                                catch (e: Exception) { android.widget.Toast.makeText(context, "Failed", android.widget.Toast.LENGTH_SHORT).show() }
                            }
                        }
                    }, Modifier.size(36.dp)) {
                        Icon(Icons.Default.Send, null, Modifier.size(18.dp),
                            if (commentText.isNotBlank()) TextHi else TextMd)
                    }
                }

                }
            }
        }
    }

    // Hitchhiker Buttons outside the card
        if (drop.isHitchhiker && !isHoldingPhoto) {
            Spacer(modifier = Modifier.height(16.dp))
            val isCarrying = drop.carriedByUserId == currentUserId
            androidx.compose.material3.Button(
                onClick = {
                    if (isCarrying) {
                        // Pass it on (requires location)
                        try {
                            val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                            val token = com.google.android.gms.tasks.CancellationTokenSource().token
                            client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, token)
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        scope.launch {
                                            val success = repository.passOnHitchhiker(drop.id, loc.latitude, loc.longitude, null, context)
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Passed it on!", android.widget.Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed to drop", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "Waiting for GPS...", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } catch (e: SecurityException) {
                            android.widget.Toast.makeText(context, "Location permission needed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Pick up
                        scope.launch {
                            val success = repository.pickUpHitchhiker(drop.id)
                            if (success) {
                                android.widget.Toast.makeText(context, "Drop picked up! Check your Profile.", android.widget.Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to pick up", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A0DAD)
                )
            ) {
                Icon(
                    if (isCarrying) Icons.Default.LocationOn
                    else Icons.Default.Add, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isCarrying) "Pass It On Here" else "Pick Up Hitchhiker", fontWeight = FontWeight.Bold)
            }
        }
    }
}

    DisposableEffect(Unit) {
        onDispose { runCatching { player?.stop(); player?.release() }; isAudioPlaying = false }
    }
}

@Composable
private fun DetailWaveform(active: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "w")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "p")
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        repeat(16) { i ->
            val d = abs(sin((i / 3.5f + p) * Math.PI)).toFloat()
            val h = if (active) 0.2f + d * 0.8f else 0.15f
            Box(Modifier.width(2.dp).height((3 + h * 14f).dp).clip(RoundedCornerShape(1.dp))
                .background(if (active) TextMd else TextLo))
        }
    }
}

private fun playAudio(context: android.content.Context, path: String, onComplete: () -> Unit): MediaPlayer? {
    return runCatching {
        MediaPlayer().apply {
            if (path.startsWith("/")) setDataSource(path) else setDataSource(context, Uri.parse(path))
            prepare(); start()
            setOnCompletionListener { mp -> onComplete(); mp.release() }
        }
    }.getOrNull()
}

private fun formatTimestamp(ts: Long): String {
    val d = System.currentTimeMillis() - ts
    return when {
        d < 60_000 -> "now"
        d < 3_600_000 -> "${d / 60_000}m"
        d < 86_400_000 -> "${d / 3_600_000}h"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }
}
