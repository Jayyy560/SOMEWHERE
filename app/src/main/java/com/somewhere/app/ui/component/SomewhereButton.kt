package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors

enum class SomewhereButtonHierarchy {
    Primary,
    Secondary,
    Destructive
}

/**
 * Minimal button with thin border. No fill, no elevation.
 * Uppercase label with wide letter-spacing.
 */
@Composable
fun SomewhereButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hierarchy: SomewhereButtonHierarchy = SomewhereButtonHierarchy.Primary,
    minWidth: Dp = 200.dp,
    height: Dp = 52.dp,
    leadingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val contentColor = when (hierarchy) {
        SomewhereButtonHierarchy.Primary -> SomewhereColors.TextPrimary
        SomewhereButtonHierarchy.Secondary -> SomewhereColors.TextSecondary
        SomewhereButtonHierarchy.Destructive -> SomewhereColors.Error
    }
    val borderColor = when (hierarchy) {
        SomewhereButtonHierarchy.Primary -> SomewhereColors.TextSecondary
        SomewhereButtonHierarchy.Secondary -> SomewhereColors.CardBorder
        SomewhereButtonHierarchy.Destructive -> SomewhereColors.Error.copy(alpha = 0.75f)
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .widthIn(min = minWidth),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(
            if (hierarchy == SomewhereButtonHierarchy.Primary) 1.dp else 0.5.dp,
            if (enabled) borderColor else SomewhereColors.CardBorder
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = SomewhereColors.TextMuted
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
    ) {
        leadingIcon?.let {
            it()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
