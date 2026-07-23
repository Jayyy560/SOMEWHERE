package com.somewhere.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.UserViewModel
import com.somewhere.app.viewmodel.NotificationItem
import com.somewhere.app.ui.component.SomewhereTopAppBar
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.util.NetworkStatus
import com.somewhere.app.util.rememberNetworkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onOpenDrop: (String) -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val userViewModel: UserViewModel = hiltViewModel(activity)
    val notifications by userViewModel.notifications.collectAsState()
    val isLoading by userViewModel.notificationsLoading.collectAsState()
    val loadError by userViewModel.notificationsError.collectAsState()
    val networkStatus = rememberNetworkStatus()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        com.somewhere.app.util.NotificationHelper.checkAndRequestPermission(activity)
        userViewModel.markNotificationsAsSeen()
    }

    Scaffold(
        topBar = {
            SomewhereTopAppBar(
                title = "Notifications",
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBack()
                }
            )
        },
        containerColor = SomewhereColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SomewhereColors.Background)
        ) {
            if (networkStatus == NetworkStatus.UNAVAILABLE) {
                Surface(color = SomewhereColors.Card, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Offline — showing saved notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = SomewhereColors.TextSecondary
                        )
                        TextButton(onClick = userViewModel::loadNotifications) {
                            Text("Retry")
                        }
                    }
                }
            }

            when {
                loadError != null && notifications.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        loadError ?: "Could not load notifications",
                        color = SomewhereColors.TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    SomewhereButton(
                        text = "Retry",
                        onClick = userViewModel::loadNotifications,
                        enabled = !isLoading
                    )
                }
                notifications.isEmpty() && isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SomewhereColors.Accent)
                }
                notifications.isEmpty() -> NotificationEmptyState(modifier = Modifier.fillMaxSize())
                else -> {
                androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        userViewModel.loadNotifications()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notif ->
                            NotificationRow(
                                notification = notif,
                                onOpen = { notif.dropId?.let(onOpenDrop) },
                                onDelete = { userViewModel.removeNotification(notif.id) }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = notification.dropId != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpen()
                }
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = resolveMinimalIcon(notification),
                contentDescription = null,
                tint = SomewhereColors.TextPrimary,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SomewhereColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SomewhereColors.TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notification.time.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = SomewhereColors.TextMuted
                )
            }

            if (notification.dropId != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open related drop",
                    tint = SomewhereColors.TextSecondary
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete notification",
                    tint = SomewhereColors.TextMuted
                )
            }
        }
        
        // Minimal divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(0.5.dp)
                .background(SomewhereColors.CardBorder)
        )
    }
}

@Composable
private fun NotificationEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NO NOTIFICATIONS",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = SomewhereColors.TextMuted
        )
    }
}

private fun resolveMinimalIcon(notification: NotificationItem): ImageVector {
    val titleLower = notification.title.lowercase()
    val msgLower = notification.message.lowercase()
    return when {
        titleLower.contains("like") || msgLower.contains("like") -> Icons.Default.FavoriteBorder
        titleLower.contains("comment") || msgLower.contains("comment") -> Icons.Default.ChatBubbleOutline
        titleLower.contains("follow") || msgLower.contains("follow") -> Icons.Default.PersonOutline
        titleLower.contains("nearby") || msgLower.contains("discover") -> Icons.Default.LocationOn
        else -> Icons.Default.NotificationsNone
    }
}
