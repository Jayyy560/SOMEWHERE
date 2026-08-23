package com.somewhere.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.ui.theme.SomewhereColors

/**
 * Fold is expressed as one useful surface, not as an illustration of origami.
 * The lower plane carries the page's story and primary action; the crease is
 * therefore part of the information hierarchy.
 */
@Composable
fun FoldHomeExperience(
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = "SOMEWHERE",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 142.dp),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.W200,
                letterSpacing = 8.sp
            ),
            color = SomewhereColors.TextPrimary,
            maxLines = 1
        )

        Canvas(Modifier.fillMaxSize()) {
            val foldTop = size.height * 0.47f
            val plane = Path().apply {
                moveTo(0f, foldTop + 28.dp.toPx())
                lineTo(size.width * 0.68f, foldTop)
                lineTo(size.width, foldTop + 12.dp.toPx())
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(plane, SomewhereColors.Surface)
            drawLine(
                color = SomewhereColors.CardBorder,
                start = Offset(0f, foldTop + 28.dp.toPx()),
                end = Offset(size.width * 0.68f, foldTop),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = SomewhereColors.AccentPurple.copy(alpha = 0.65f),
                start = Offset(size.width * 0.68f, foldTop),
                end = Offset(size.width, foldTop + 12.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 118.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "HERE / NOW",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = SomewhereColors.TextMuted
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Find what this place remembers.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.W400
                ),
                color = SomewhereColors.TextPrimary
            )
            Spacer(Modifier.height(30.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(SomewhereColors.Card)
                    .clickable(onClick = onExplore)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPLORE NEARBY",
                    style = MaterialTheme.typography.labelLarge,
                    color = SomewhereColors.TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = SomewhereColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FoldNotificationIcon(
    hasNotification: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = SomewhereColors.TextPrimary,
                modifier = Modifier.size(21.dp)
            )
            if (hasNotification) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(SomewhereColors.AccentPurple)
                )
            }
        }
    }
}
