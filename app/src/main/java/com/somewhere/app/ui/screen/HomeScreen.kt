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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.net.Uri
import com.somewhere.app.data.remote.SupabaseManager
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
@Composable
fun HomeScreen(
    onExplore: () -> Unit,
    onDrop: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
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

    LaunchedEffect(Unit) { 
        visible = true 
        val user = SupabaseManager.client.auth.currentUserOrNull()
        val name = user?.userMetadata?.get("name")?.jsonPrimitive?.content
        val gender = user?.userMetadata?.get("gender")?.jsonPrimitive?.content
        if (name.isNullOrBlank() || gender.isNullOrBlank()) {
            showNamePrompt = true
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onProfile) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = SomewhereColors.TextPrimary
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SomewhereColors.TextPrimary
                )
            }
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
            PressableButton(
                text = "Explore",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExplore()
                }
            )
            PressableButton(
                text = "Leave something here",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDrop()
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
                                        avatarUri = newAvatarUri,
                                        onComplete = { showNamePrompt = false }
                                    )
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

