package com.somewhere.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.somewhere.app.ui.component.DropDetailSheet
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.viewmodel.NotificationDropViewModel

@Composable
fun NotificationDropScreen(
    dropId: String,
    onBack: () -> Unit,
    onFindSpot: (String) -> Unit,
    viewModel: NotificationDropViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dropId) {
        viewModel.load(dropId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(color = SomewhereColors.Accent)
            uiState.error != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SomewhereColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                SomewhereButton(text = "Retry", onClick = { viewModel.load(dropId) })
                Spacer(Modifier.height(12.dp))
                SomewhereButton(text = "Back", onClick = onBack)
            }
            uiState.drop != null -> {
                val drop = uiState.drop ?: return@Box
                DropDetailSheet(
                    drop = drop,
                    onDismiss = onBack,
                    onDelete = {
                        viewModel.delete(drop)
                        onBack()
                    },
                    onReport = {
                        viewModel.report(drop.id)
                        onBack()
                    },
                    onBlock = {
                        viewModel.block(drop.authorName)
                        onBack()
                    },
                    onFindSpot = onFindSpot
                )
            }
        }
    }
}
