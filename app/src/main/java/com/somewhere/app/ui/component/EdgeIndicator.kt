package com.somewhere.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors

@Composable
fun EdgeIndicator(isLeft: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(CircleShape)
            .background(SomewhereColors.GlassBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLeft) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Turn Left", tint = SomewhereColors.TextPrimary)
            Text("Drops this way", color = SomewhereColors.TextPrimary)
        } else {
            Text("Drops this way", color = SomewhereColors.TextPrimary)
            Icon(Icons.Default.ChevronRight, contentDescription = "Turn Right", tint = SomewhereColors.TextPrimary)
        }
    }
}
