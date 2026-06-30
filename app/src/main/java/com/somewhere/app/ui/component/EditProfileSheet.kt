package com.somewhere.app.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.somewhere.app.R
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    initialName: String,
    initialGender: String,
    initialAvatarUrl: String?,
    viewModel: UserViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(if (initialGender.isBlank()) "Other" else initialGender) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val isSaving by viewModel.isSaving.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SomewhereColors.Surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                color = SomewhereColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar picker
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SomewhereColors.Card)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (initialAvatarUrl == "default_female") {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.default_female_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (initialAvatarUrl == "default_male" || initialAvatarUrl == null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.default_male_avatar),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = initialAvatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Overlay text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Change", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Gender (for default avatars)",
                style = MaterialTheme.typography.labelMedium,
                color = SomewhereColors.TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Male", "Female", "Other").forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = gender == option,
                            onClick = { gender = option },
                            colors = RadioButtonDefaults.colors(selectedColor = SomewhereColors.Accent)
                        )
                        Text(text = option, color = SomewhereColors.TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SomewhereButton(
                text = if (isSaving) "Saving..." else "Save Profile",
                onClick = {
                    viewModel.saveProfile(name, gender, selectedImageUri) { error ->
                        if (error == null) {
                            onDismiss()
                        } else {
                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSaving && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
