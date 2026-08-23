package com.somewhere.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.ui.theme.AppTheme
import com.somewhere.app.ui.theme.SomewhereColors

@Composable
fun ThemeSelector(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeChoiceCard(
            theme = AppTheme.LIQUID,
            selected = selectedTheme == AppTheme.LIQUID,
            onClick = { onThemeSelected(AppTheme.LIQUID) }
        )
        ThemeChoiceCard(
            theme = AppTheme.FOLD,
            selected = selectedTheme == AppTheme.FOLD,
            onClick = { onThemeSelected(AppTheme.FOLD) }
        )
    }
}

@Composable
private fun ThemeChoiceCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val previewBackground = if (theme == AppTheme.LIQUID) Color(0xFF0A0A0A) else Color(0xFF0B0C0A)
    val previewSurface = if (theme == AppTheme.LIQUID) Color(0xFF17171C) else Color(0xFF20211C)
    val previewText = if (theme == AppTheme.LIQUID) Color(0xFFEBEBEB) else Color(0xFFF1EFE7)
    val previewAccent = if (theme == AppTheme.LIQUID) Color(0xFFB7A5CC) else Color(0xFFD3C6AA)
    val shape = if (theme == AppTheme.LIQUID) RoundedCornerShape(18.dp) else RoundedCornerShape(0.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(shape)
            .background(previewBackground)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SomewhereColors.AccentPurple else SomewhereColors.CardBorder,
                shape = shape
            )
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(76.dp),
            contentAlignment = Alignment.Center
        ) {
            if (theme == AppTheme.LIQUID) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.06f))
                        .background(
                            Brush.radialGradient(
                                listOf(previewAccent.copy(alpha = 0.42f), Color.Transparent)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .width(104.dp)
                        .height(30.dp)
                        .background(previewSurface)
                        .border(
                            1.dp,
                            previewText.copy(alpha = 0.45f),
                            RoundedCornerShape(0.dp)
                        )
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .width(104.dp)
                        .height(42.dp)
                        .background(previewSurface)
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(104.dp)
                        .height(1.dp)
                        .background(previewAccent)
                )
            }
        }

        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = theme.displayName,
                color = previewText,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (theme == AppTheme.LIQUID) "Original liquid interface" else "Planes that crease and unfold",
                color = previewText.copy(alpha = 0.66f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            )
        }

        if (selected) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(if (theme == AppTheme.LIQUID) CircleShape else RoundedCornerShape(0.dp))
                    .background(previewAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF15140F),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
