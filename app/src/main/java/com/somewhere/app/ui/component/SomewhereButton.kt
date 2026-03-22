package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors

/**
 * Minimal button with thin border. No fill, no elevation.
 * Uppercase label with wide letter-spacing.
 */
@Composable
fun SomewhereButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .widthIn(min = 200.dp),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(
            0.5.dp,
            if (enabled) SomewhereColors.CardBorder else SomewhereColors.TextMuted
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SomewhereColors.Background,
            contentColor = SomewhereColors.TextPrimary,
            disabledContainerColor = SomewhereColors.Background,
            disabledContentColor = SomewhereColors.TextMuted
        )
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
