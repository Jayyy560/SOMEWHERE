package com.somewhere.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.UserViewModel
import com.somewhere.app.viewmodel.NotificationItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val userViewModel: UserViewModel = hiltViewModel(activity)
    
    val notifications by userViewModel.notifications.collectAsState()
    
    LaunchedEffect(Unit) {
        userViewModel.markNotificationsAsSeen()
    }
    
    var showClearConfirmDialog by remember { mutableStateOf<NotificationItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = SomewhereColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SomewhereColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SomewhereColors.Background)
            )
        },
        containerColor = SomewhereColors.Background,
        modifier = Modifier.fillMaxSize().systemBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "No notifications",
                        modifier = Modifier.size(64.dp),
                        tint = SomewhereColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You're all caught up.\nNo new notifications.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SomewhereColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    com.somewhere.app.ui.component.SomewhereButton(
                        text = "Back to map",
                        onClick = onBack
                    )
                }
            } else {
                androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { 
                        isRefreshing = true
                        userViewModel.loadNotifications()
                        scope.launch {
                            kotlinx.coroutines.delay(800)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notifications) { notif ->
                            NotificationCard(
                                notification = notif,
                                onClearRequest = { showClearConfirmDialog = notif }
                            )
                        }
                    }
                }
            }
        }
    }

    showClearConfirmDialog?.let { notif ->
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = null },
            title = { Text("Clear Notification") },
            text = { Text("Are you sure you want to clear this notification?") },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.removeNotification(notif.id)
                    showClearConfirmDialog = null
                }) {
                    Text("Clear", color = SomewhereColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = null }) {
                    Text("Cancel", color = SomewhereColors.TextPrimary)
                }
            },
            containerColor = SomewhereColors.Card,
            titleContentColor = SomewhereColors.TextPrimary,
            textContentColor = SomewhereColors.TextSecondary
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClearRequest: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SomewhereColors.Card)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    expanded = true
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SomewhereColors.AccentPurple.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notification Icon",
                tint = SomewhereColors.AccentPurple,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = SomewhereColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = SomewhereColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.time,
                style = MaterialTheme.typography.labelSmall,
                color = SomewhereColors.TextMuted
            )
        }

        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = SomewhereColors.TextSecondary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = SomewhereColors.Surface
            ) {
                DropdownMenuItem(
                    text = { Text("Clear Notification", color = SomewhereColors.Error) },
                    onClick = {
                        expanded = false
                        onClearRequest()
                    }
                )
            }
        }
    }
}
