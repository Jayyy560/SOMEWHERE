package com.somewhere.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.ui.theme.AppTheme
import com.somewhere.app.ui.theme.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomewhereTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    val isFold = LocalAppTheme.current == AppTheme.FOLD
    TopAppBar(
        modifier = modifier.then(
            if (isFold) {
                Modifier.drawBehind {
                    drawLine(
                        color = SomewhereColors.CardBorder,
                        start = Offset(0f, size.height - 1f),
                        end = Offset(size.width, size.height - 1f),
                        strokeWidth = 1f
                    )
                }
            } else Modifier
        ),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = SomewhereColors.TextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SomewhereColors.TextPrimary
                )
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isFold) SomewhereColors.Surface else SomewhereColors.Background,
            scrolledContainerColor = if (isFold) SomewhereColors.Surface else SomewhereColors.Background,
            navigationIconContentColor = SomewhereColors.TextPrimary,
            titleContentColor = SomewhereColors.TextPrimary,
            actionIconContentColor = SomewhereColors.TextPrimary
        )
    )
}
