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
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.ProfileViewModel
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.somewhere.app.R

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val myDrops by viewModel.myDrops.collectAsState()
    var selectedDrop by remember { mutableStateOf<Drop?>(null) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SomewhereColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${myDrops.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SomewhereColors.TextPrimary
                        )
                        Text(
                            text = "Drops",
                            style = MaterialTheme.typography.bodySmall,
                            color = SomewhereColors.TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Grid
            if (myDrops.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You haven't dropped anything yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SomewhereColors.TextSecondary
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
                    items(myDrops) { drop ->
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
