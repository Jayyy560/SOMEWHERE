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
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.somewhere.app.data.model.Drop
import com.somewhere.app.data.remote.SupabaseManager
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.component.shimmerEffect
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.ProfileViewModel
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.somewhere.app.R

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    var selectedDrop by remember { mutableStateOf<Drop?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) {
            viewModel.refreshUnlockedDrops()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                                viewModel.logOut { onBack() }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Account Data") },
                            onClick = {
                                showSettingsMenu = false
                                viewModel.deleteAccount { onBack() }
                            }
                        )
                    }
                }
            }

            // Profile Info Header
            val user = SupabaseManager.client.auth.currentUserOrNull()
            val name = user?.userMetadata?.get("name")?.jsonPrimitive?.content ?: "Profile"
            val avatarUrl = user?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content

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
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Grid
            val currentList = if (selectedTabIndex == 0) myDrops else unlockedDrops
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
                    Icon(
                        imageVector = if (selectedTabIndex == 0) androidx.compose.material.icons.Icons.Default.LocationOn else androidx.compose.material.icons.Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SomewhereColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTabIndex == 0) "It's quiet here...\nBe the first to leave a mark!" else "You haven't discovered any drops yet.\nGet out there and explore!",
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

        selectedDrop?.let { drop ->
            DropDetailSheet(
                drop = drop,
                onDismiss = { selectedDrop = null },
                onDelete = {
                    viewModel.deleteDrop(drop)
                    selectedDrop = null
                },
                onBlock = {},
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
