package com.somewhere.app.ui.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.SettingsUtils
import com.somewhere.app.util.PermissionRequestHistory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    title: String,
    description: String,
    permissions: List<String>,
    onGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(permissions)
    var requestHistoryVersion by remember { mutableIntStateOf(0) }

    val allGranted = permissionsState.allPermissionsGranted
    val shouldShowRationale = permissionsState.permissions.any { permission ->
        when (val status = permission.status) {
            is PermissionStatus.Denied -> status.shouldShowRationale
            else -> false
        }
    }
    val deniedPermissions = permissionsState.permissions.filter {
        it.status is PermissionStatus.Denied
    }
    val permanentlyDenied = remember(
        requestHistoryVersion,
        deniedPermissions,
        shouldShowRationale,
        allGranted
    ) {
        deniedPermissions.isNotEmpty() &&
            deniedPermissions.all {
                PermissionRequestHistory.wasRequested(context, it.permission)
            } &&
            !shouldShowRationale &&
            !allGranted
    }

    if (allGranted) {
        onGranted()
        return
    }

    PermissionFallback(
        context = context,
        title = title,
        description = description,
        showRationale = shouldShowRationale,
        permanentlyDenied = permanentlyDenied,
        onRequest = {
            PermissionRequestHistory.markRequested(context, permissions)
            requestHistoryVersion++
            permissionsState.launchMultiplePermissionRequest()
        }
    )
}

@Composable
private fun PermissionFallback(
    context: Context,
    title: String,
    description: String,
    showRationale: Boolean,
    permanentlyDenied: Boolean,
    onRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge
            )

            SomewhereButton(
                text = if (permanentlyDenied) "Open settings" else "Allow access",
                onClick = {
                    if (permanentlyDenied) {
                        SettingsUtils.openAppSettings(context)
                    } else {
                        onRequest()
                    }
                }
            )

            if (showRationale) {
                Text(
                    text = "You can change this anytime in Settings.",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
