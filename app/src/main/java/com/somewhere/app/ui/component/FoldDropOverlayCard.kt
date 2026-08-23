package com.somewhere.app.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.LocationUtils
import com.somewhere.app.viewmodel.DiscoveredDrop

/** A Drop rendered as a closed crease that unfolds into two joined planes. */
@Composable
internal fun FoldDropOverlayCard(
    item: DiscoveredDrop,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetWidth = if (item.isUnlocked) 226.dp else 116.dp
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "foldDropWidth"
    )
    val height by animateDpAsState(
        targetValue = if (item.isUnlocked) 92.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "foldDropHeight"
    )
    val openAmount by animateFloatAsState(
        targetValue = if (item.isUnlocked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "foldDropOpen"
    )
    val primaryScale = if (item.isPrimary) 1f else 0.86f

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = primaryScale
                scaleY = primaryScale
                rotationY = (1f - openAmount) * -8f
                cameraDistance = 18f * density
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .shadow(
                elevation = if (item.isUnlocked) 14.dp else 5.dp,
                shape = RectangleShape,
                ambientColor = SomewhereColors.TextPrimary.copy(alpha = 0.24f),
                spotColor = SomewhereColors.TextPrimary.copy(alpha = 0.3f)
            )
            .background(if (item.isUnlocked) SomewhereColors.Surface else androidx.compose.ui.graphics.Color.Transparent)
            .drawBehind {
                if (!item.isUnlocked) {
                    val y = size.height / 2f
                    drawLine(
                        color = SomewhereColors.CardBorder,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = SomewhereColors.AccentPurple,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.42f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.58f, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .then(
                if (item.isUnlocked) {
                    Modifier.border(1.dp, SomewhereColors.CardBorder, RectangleShape)
                } else {
                    Modifier
                }
            )
            .then(if (item.isUnlocked) Modifier.clickable(onClick = onTap) else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        if (item.isUnlocked) {
            Row(
                modifier = Modifier.width(226.dp).fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                        .background(SomewhereColors.Card)
                ) {
                    AsyncImage(
                        model = item.drop.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxHeight().width(70.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(SomewhereColors.TextPrimary.copy(alpha = 0.14f))
                    )
                }

                Column(
                    modifier = Modifier
                        .width(156.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .alpha(openAmount),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = LocationUtils.formatDistance(item.distanceMeters).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = SomewhereColors.TextSecondary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "OPEN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = SomewhereColors.TextPrimary
                        )
                    }
                    Text(
                        text = item.drop.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.W500,
                            lineHeight = 17.sp
                        ),
                        color = SomewhereColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(1.dp)
                            .background(SomewhereColors.CardBorder)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(10.dp),
                    tint = SomewhereColors.TextSecondary
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "${item.distanceMeters.toInt()} M",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SomewhereColors.TextPrimary
                )
            }
        }
    }
}
