package com.somewhere.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.somewhere.app.R
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.remote.SupabaseManager
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.component.EditProfileSheet
import com.somewhere.app.ui.component.shimmerEffect
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.ProfileViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.somewhere.app.viewmodel.UserViewModel
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val myDrops by viewModel.myDrops.collectAsState()
    val unlockedDrops by viewModel.unlockedDrops.collectAsState()
    val carriedDrops by viewModel.carriedDrops.collectAsState()
    var selectedDrop by remember { mutableStateOf<Drop?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) {
            viewModel.refreshUnlockedDrops()
        }
    }

    Scaffold(
        bottomBar = {
            // Reusing the simple text bottom nav for now, or just leave empty since it's handled by main
        },
        containerColor = SomewhereColors.Background,
        modifier = Modifier
            .background(SomewhereColors.Background)
            .systemBarsPadding()
    ) {
        var showEditProfile by remember { mutableStateOf(false) }
        var showLogoutConfirm by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsState(initial = io.github.jan.supabase.gotrue.SessionStatus.NotAuthenticated(false))
        val user = (sessionStatus as? io.github.jan.supabase.gotrue.SessionStatus.Authenticated)?.session?.user
        val name = user?.userMetadata?.get("name")?.jsonPrimitive?.content ?: "Profile"
        val gender = user?.userMetadata?.get("gender")?.jsonPrimitive?.content ?: "Other"
        val avatarUrl = user?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
        val currentUserId = user?.id ?: ""

        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                Box {
                    IconButton(onClick = { showSettingsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = SomewhereColors.TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Log Out") },
                            onClick = {
                                showSettingsMenu = false
                                showLogoutConfirm = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Account Data") },
                            onClick = {
                                showSettingsMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            // Profile Info Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = SomewhereColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                androidx.compose.material3.OutlinedButton(
                    onClick = { showEditProfile = true },
                    modifier = Modifier.height(36.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = SomewhereColors.TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SomewhereColors.TextMuted)
                ) {
                    Text("Edit Profile", style = MaterialTheme.typography.labelMedium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SomewhereColors.Background,
                    contentColor = SomewhereColors.TextPrimary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("My Drops (${myDrops.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Discovered (${unlockedDrops.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Backpack (${carriedDrops.size})") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Grid
            val currentList = when (selectedTabIndex) {
                0 -> myDrops
                1 -> unlockedDrops
                else -> carriedDrops
            }
            val isLoading by viewModel.isLoading.collectAsState()

            if (isLoading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(12) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .shimmerEffect()
                        )
                    }
                }
            } else if (currentList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val emptyIcon = when (selectedTabIndex) {
                        0 -> Icons.Default.LocationOn
                        1 -> Icons.Default.LockOpen
                        else -> Icons.Default.List
                    }
                    val emptyText = when (selectedTabIndex) {
                        0 -> "It's quiet here...\nBe the first to leave a mark!"
                        1 -> "You haven't discovered any drops yet.\nGet out there and explore!"
                        else -> "Your backpack is empty.\nPick up a Hitchhiker Drop!"
                    }
                    Icon(
                        imageVector = emptyIcon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SomewhereColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SomewhereColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(currentList) { drop ->
                        ProfileGridItem(
                            drop = drop,
                            onClick = { selectedDrop = drop }
                        )
                    }
                }
            }
        }

        val activity = LocalContext.current as ComponentActivity
        val userViewModel: UserViewModel = hiltViewModel(activity)

        if (showEditProfile) {
            EditProfileSheet(
                initialName = name,
                initialGender = gender,
                initialAvatarUrl = avatarUrl,
                viewModel = userViewModel,
                onDismiss = { showEditProfile = false }
            )
        }

        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutConfirm = false
                        viewModel.logOut { onBack() }
                    }) {
                        Text("Log Out", color = SomewhereColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text("Cancel", color = SomewhereColors.TextPrimary)
                    }
                },
                containerColor = SomewhereColors.Surface,
                titleContentColor = SomewhereColors.TextPrimary,
                textContentColor = SomewhereColors.TextSecondary
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Account Data") },
                text = { Text("Are you sure you want to delete your account data? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount { onBack() }
                    }) {
                        Text("Delete", color = SomewhereColors.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = SomewhereColors.TextPrimary)
                    }
                },
                containerColor = SomewhereColors.Surface,
                titleContentColor = SomewhereColors.TextPrimary,
                textContentColor = SomewhereColors.TextSecondary
            )
        }

        selectedDrop?.let { drop ->
            DropDetailSheet(
                drop = drop,
                onDismiss = { selectedDrop = null },
                onDelete = {
                    viewModel.deleteDrop(drop)
                    selectedDrop = null
                },
                onBlock = { 
                    selectedDrop?.authorName?.let { viewModel.blockUser(it) }
                    selectedDrop = null 
                },
                onReport = {
                    selectedDrop = null
                }
            )
        }
    }
}

@Composable
fun ProfileGridItem(drop: Drop, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(SomewhereColors.Card)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = drop.imageUrl,
            contentDescription = "Drop photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
