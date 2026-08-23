package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.ui.theme.AppTheme
import com.somewhere.app.ui.theme.LocalAppTheme

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
    if (LocalAppTheme.current == AppTheme.FOLD) {
        FoldSomewhereButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            hierarchy = hierarchy,
            minWidth = minWidth,
            height = height,
            leadingIcon = leadingIcon,
            interactionSource = interactionSource
        )
        return
    }

    LiquidSomewhereButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        hierarchy = hierarchy,
        minWidth = minWidth,
        height = height,
        leadingIcon = leadingIcon,
        interactionSource = interactionSource
    )
}

@Composable
private fun LiquidSomewhereButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    hierarchy: SomewhereButtonHierarchy,
    minWidth: Dp,
    height: Dp,
    leadingIcon: (@Composable () -> Unit)?,
    interactionSource: MutableInteractionSource
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

@Composable
private fun FoldSomewhereButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    hierarchy: SomewhereButtonHierarchy,
    minWidth: Dp,
    height: Dp,
    leadingIcon: (@Composable () -> Unit)?,
    interactionSource: MutableInteractionSource
) {
    val contentColor = when (hierarchy) {
        SomewhereButtonHierarchy.Primary -> SomewhereColors.TextPrimary
        SomewhereButtonHierarchy.Secondary -> SomewhereColors.TextSecondary
        SomewhereButtonHierarchy.Destructive -> SomewhereColors.Error
    }
    val ruleColor = when (hierarchy) {
        SomewhereButtonHierarchy.Primary -> SomewhereColors.CardBorder
        SomewhereButtonHierarchy.Secondary -> SomewhereColors.Divider
        SomewhereButtonHierarchy.Destructive -> SomewhereColors.Error.copy(alpha = 0.6f)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .widthIn(min = minWidth)
            .drawBehind {
                drawLine(
                    color = ruleColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        shape = RectangleShape,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (hierarchy == SomewhereButtonHierarchy.Primary) {
                SomewhereColors.Card
            } else {
                Color.Transparent
            },
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = SomewhereColors.TextMuted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge)
            if (hierarchy == SomewhereButtonHierarchy.Primary) {
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
