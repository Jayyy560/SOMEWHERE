package com.somewhere.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.somewhere.app.ui.component.SomewhereButton
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SomewhereColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SomewhereColors.Background)
                .padding(padding)
                .systemBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Manage permissions and data on this device.",
                style = MaterialTheme.typography.bodyLarge
            )

            SomewhereButton(
                text = "Manage permissions",
                onClick = { SettingsUtils.openAppSettings(context) }
            )

            SomewhereButton(
                text = if (uiState.isClearing) "Clearing..." else "Delete all drops",
                enabled = !uiState.isClearing,
                onClick = { showConfirm = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            SomewhereButton(
                text = "Back",
                onClick = onBack
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
}
