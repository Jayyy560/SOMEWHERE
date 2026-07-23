package com.somewhere.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.hilt.navigation.compose.hiltViewModel
import com.somewhere.app.BuildConfig
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.component.SomewhereTopAppBar
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.SettingsUtils
import com.somewhere.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val blockedUsers by viewModel.blockedUsers.collectAsState()
    var showBlockedUsers by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SomewhereTopAppBar(title = "Settings", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SomewhereColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SomewhereColors.Background)
                .padding(padding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val openLegalUrl = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://somewhere-privacy-policy.vercel.app/"))
                context.startActivity(intent)
            }

            Text("ACCOUNT", style = MaterialTheme.typography.labelMedium, color = SomewhereColors.TextSecondary)
            SomewhereButton(text = "Manage Permissions", onClick = { SettingsUtils.openAppSettings(context) })
            SomewhereButton(text = "Blocked Users (${blockedUsers.size})", onClick = { showBlockedUsers = true })

            HorizontalDivider(color = SomewhereColors.Card)

            Text("LEGAL", style = MaterialTheme.typography.labelMedium, color = SomewhereColors.TextSecondary)
            SomewhereButton(text = "Privacy Policy", onClick = { openLegalUrl() })
            SomewhereButton(text = "Terms of Service", onClick = { openLegalUrl() })
            SomewhereButton(text = "Community Guidelines", onClick = { openLegalUrl() })

            HorizontalDivider(color = SomewhereColors.Card)

            Text("DANGER ZONE", style = MaterialTheme.typography.labelMedium, color = SomewhereColors.TextSecondary)
            SomewhereButton(
                text = if (uiState.isClearing) "Clearing..." else "Delete all drops",
                enabled = !uiState.isClearing,
                onClick = { showConfirm = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = SomewhereColors.TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete all drops?") },
            text = { Text("This removes all stored drops and their photos from this device.") },
            confirmButton = {
                SomewhereButton(
                    text = "Delete",
                    onClick = {
                        showConfirm = false
                        viewModel.deleteAllDrops()
                    }
                )
            },
            dismissButton = {
                SomewhereButton(
                    text = "Cancel",
                    onClick = { showConfirm = false }
                )
            }
        )
    }

    if (showBlockedUsers) {
        AlertDialog(
            onDismissRequest = { showBlockedUsers = false },
            title = { Text("Blocked Users") },
            text = {
                if (blockedUsers.isEmpty()) {
                    Text("No blocked users.")
                } else {
                    LazyColumn {
                        items(blockedUsers.toList()) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user)
                                androidx.compose.material3.TextButton(onClick = { viewModel.unblockUser(user) }) {
                                    Text("Unblock", color = SomewhereColors.Accent)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                SomewhereButton(
                    text = "Close",
                    onClick = { showBlockedUsers = false }
                )
            }
        )
    }
}
