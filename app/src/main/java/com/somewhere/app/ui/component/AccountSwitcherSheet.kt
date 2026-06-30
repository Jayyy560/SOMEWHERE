package com.somewhere.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.somewhere.app.data.local.AccountStore
import com.somewhere.app.data.local.SavedAccount
import com.somewhere.app.data.remote.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    onAccountSwitched: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var savedAccounts by remember { mutableStateOf(AccountStore.getSavedAccounts()) }
    var currentUserId by remember { mutableStateOf(SupabaseManager.client.auth.currentUserOrNull()?.id) }
    var isSwitching by remember { mutableStateOf(false) }

    val accounts = savedAccounts

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )

            HorizontalDivider(color = Color.DarkGray)

            val context = LocalContext.current
            LazyColumn {
                items(accounts) { account ->
                    val isActive = account.userId == currentUserId
                    AccountRow(
                        account = account,
                        isActive = isActive,
                        onClick = {
                            if (isSwitching || isActive) return@AccountRow
                            isSwitching = true
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    SupabaseManager.importSession(account.accessToken, account.refreshToken)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        onAccountSwitched()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isSwitching = false
                                        android.widget.Toast.makeText(context, "Failed to switch account", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    HorizontalDivider(color = Color.DarkGray)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSwitching) return@clickable
                                isSwitching = true
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    SupabaseManager.saveCurrentSessionToStore()
                                    SupabaseManager.client.auth.clearSession()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        onAddAccount()
                                    }
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSwitching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Account",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Add Account",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountRow(
    account: SavedAccount,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = account.avatarUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${account.userId}",
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Active",
                tint = Color(0xFF651FFF)
            )
        }
    }
}
