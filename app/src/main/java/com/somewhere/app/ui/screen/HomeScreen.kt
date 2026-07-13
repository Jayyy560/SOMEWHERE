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
import androidx.compose.material.icons.filled.Person
import com.somewhere.app.util.rememberReduceMotionEnabled
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.net.Uri
import com.somewhere.app.data.remote.SupabaseManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.ComponentActivity
import com.somewhere.app.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonPrimitive

/**
 * Home screen — minimal entry point.
 * "SOMEWHERE" title + two action buttons.
 * Lots of negative space, calm atmosphere.
 */
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun HomeScreen(
    onExplore: () -> Unit,
    onSettings: () -> Unit,
    onNotifications: () -> Unit,
    onTripMode: () -> Unit = {}
) {
    val activity = LocalContext.current as ComponentActivity
    val userViewModel: UserViewModel = hiltViewModel(activity)
    
    val focusManager = LocalFocusManager.current
    var visible by rememberSaveable { mutableStateOf(false) }
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
    
    var showNamePrompt by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newGender by remember { mutableStateOf("Male") }
    var newAvatarUri by remember { mutableStateOf<Uri?>(null) }
    val isSaving by userViewModel.isSaving.collectAsState()
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newAvatarUri = uri
    }

    val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsState(initial = io.github.jan.supabase.gotrue.SessionStatus.NotAuthenticated(false))
    val user = (sessionStatus as? io.github.jan.supabase.gotrue.SessionStatus.Authenticated)?.session?.user

    LaunchedEffect(user) { 
        visible = true 
        if (user != null) {
            val name = user.userMetadata?.get("name")?.jsonPrimitive?.content
            val gender = user.userMetadata?.get("gender")?.jsonPrimitive?.content
            if (name.isNullOrBlank() || gender.isNullOrBlank()) {
                showNamePrompt = true
            } else {
                showNamePrompt = false
            }
        }
    }
    
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val hasNotifications by userViewModel.hasNotifications.collectAsState()

            NotificationDropIcon(
                hasNotification = hasNotifications,
                onClick = onNotifications
            )

            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SomewhereColors.TextPrimary
                )
            }
        }

        // Liquid Refraction Logo — positioned slightly above center
        com.somewhere.app.ui.component.LiquidLogo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .alpha(titleAlpha)
        )

        // Floating Liquid Glass Drops
        com.somewhere.app.ui.component.FloatingDropsAnimation(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.Center)
                .offset(y = 60.dp) // Offset below the title
                .alpha(titleAlpha * 0.8f) // Fade in with the title but slightly subtler
        )

        // Action buttons — lower portion
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 170.dp)
                .alpha(buttonsAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PressableButton(
                text = "Explore",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExplore()
                }
            )
        }

        if (showNamePrompt) {
            AlertDialog(
                onDismissRequest = { /* force entry */ },
                title = { Text("Complete your profile") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Let others know who dropped this! Pick a photo, name, and gender.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SomewhereColors.TextSecondary
                        )
                        Spacer(Modifier.height(16.dp))

                        // Avatar Picker
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(SomewhereColors.Card)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (newAvatarUri != null) {
                                AsyncImage(
                                    model = newAvatarUri,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Pick Avatar",
                                    tint = SomewhereColors.TextSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Male", "Female", "Other").forEach { g ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.RadioButton(
                                        selected = newGender == g,
                                        onClick = { newGender = g }
                                    )
                                    Text(text = g, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isSaving) {
                        CircularProgressIndicator(color = SomewhereColors.Accent)
                    } else {
                        SomewhereButton(
                            text = "Save",
                            onClick = {
                                if (newName.isNotBlank()) {
                                    userViewModel.saveProfile(
                                        name = newName,
                                        gender = newGender,
                                        avatarUri = newAvatarUri
                                    ) { error ->
                                        if (error == null) {
                                            showNamePrompt = false
                                        } else {
                                            android.widget.Toast.makeText(activity, error, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PressableButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )

    Box(modifier = Modifier.scale(scale)) {
        SomewhereButton(
            text = text,
            onClick = onClick,
            interactionSource = interactionSource
        )
    }
}

@Composable
fun NotificationDropIcon(hasNotification: Boolean, onClick: () -> Unit) {
    val waveTransition = rememberInfiniteTransition(label = "wave")
    val waveProgress by waveTransition.animateFloat(
        initialValue = 0f, 
        targetValue = if (hasNotification) (2 * kotlin.math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), 
            repeatMode = RepeatMode.Restart
        )
    )

    // Only apply the wave if there is a notification, otherwise static
    val shape = com.somewhere.app.ui.component.WavyPillShape(
        progress = if (hasNotification) waveProgress else 0f, 
        amplitudeMultiplier = if (hasNotification) 1.5f else 0f
    )

    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (hasNotification) {
                        Modifier
                            .shadow(
                                elevation = 8.dp,
                                shape = shape,
                                ambientColor = com.somewhere.app.ui.theme.LocalAmbientColors.current.shadowAmbient,
                                spotColor = com.somewhere.app.ui.theme.LocalAmbientColors.current.shadowSpot
                            )
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .background(Color.White.copy(alpha = 0.02f))
                .then(
                    if (hasNotification) {
                        Modifier
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        com.somewhere.app.ui.theme.LocalAmbientColors.current.glowPrimary.copy(alpha = 0.2f), 
                                        Color.Transparent
                                    ),
                                    center = androidx.compose.ui.geometry.Offset(10f, 5f),
                                    radius = 30f
                                )
                            )
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        com.somewhere.app.ui.theme.LocalAmbientColors.current.glowSecondary.copy(alpha = 0.4f), 
                                        Color.Transparent
                                    ),
                                    center = androidx.compose.ui.geometry.Offset(50f, 15f),
                                    radius = 40f
                                )
                            )
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.4f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = if (hasNotification) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White,
                                com.somewhere.app.ui.theme.LocalAmbientColors.current.glowPrimary.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.1f),
                                com.somewhere.app.ui.theme.LocalAmbientColors.current.glowSecondary.copy(alpha = 0.5f),
                                Color.White
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.5f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    },
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Intense starburst glare
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 1.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = if (hasNotification) 1f else 0.5f), Color.Transparent),
                                radius = 10f
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
