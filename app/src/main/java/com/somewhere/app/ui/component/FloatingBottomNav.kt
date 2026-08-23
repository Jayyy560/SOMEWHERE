package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import com.somewhere.app.ui.theme.AppTheme
import com.somewhere.app.ui.theme.LocalAppTheme
import com.somewhere.app.ui.theme.SomewhereColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingBottomNav(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (Int) -> Unit,
    onProfileLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        if (com.somewhere.app.PrototypeFeatures.SHOW_WAYFINDER) {
            add(Icons.Default.DirectionsWalk to "Wayfinder")
        }
        add(Icons.Default.Home to "Home")
        add(Icons.Default.Add to "Create")
        add(Icons.Default.Person to "Profile")
    }

    if (LocalAppTheme.current == AppTheme.FOLD) {
        FoldBottomNav(
            tabs = tabs,
            selectedIndex = pagerState.currentPage,
            onTabSelected = onTabSelected,
            onProfileLongClick = onProfileLongClick,
            modifier = modifier
        )
        return
    }

    var isWavy by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) {
        isWavy = true
        kotlinx.coroutines.delay(1000)
        isWavy = false
    }

    val waveTransition = rememberInfiniteTransition(label = "wave")
    val waveProgress by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isWavy) (2 * kotlin.math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )
    val indicatorShape = if (isWavy) {
        WavyPillShape(waveProgress, 1.15f)
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
    }

    Box(
        modifier = modifier
            .padding(bottom = 32.dp)
            .height(72.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f), // Subdued, barely-there reflection
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset {
                    val offsetDp = (-14).dp +
                        ((pagerState.currentPage + pagerState.currentPageOffsetFraction) * 72).dp
                    androidx.compose.ui.unit.IntOffset(x = offsetDp.roundToPx(), y = 0)
                }
                .height(64.dp)
                .width(84.dp)
                .clip(indicatorShape)
                .background(Color.White.copy(alpha = 0.02f))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            com.somewhere.app.ui.theme.LocalAmbientColors.current.glowSecondary.copy(alpha = 0.4f),
                            com.somewhere.app.ui.theme.LocalAmbientColors.current.glowPrimary.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(40f, 20f),
                        radius = 100f
                    )
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.4f)
                        )
                    ),
                    shape = indicatorShape
                )
                .blur(4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (icon, desc) ->
                BottomNavItem(
                    icon = icon,
                    description = desc,
                    isSelected = pagerState.currentPage == index,
                    onClick = { onTabSelected(index) },
                    onLongClick = {
                        if (desc == "Profile") {
                            onProfileLongClick()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoldBottomNav(
    tabs: List<Pair<ImageVector, String>>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onProfileLongClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(SomewhereColors.Surface)
            .drawBehind {
                drawLine(
                    color = SomewhereColors.CardBorder,
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            FoldNavItem(
                icon = icon,
                label = label,
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                onLongClick = {
                    if (label == "Profile") onProfileLongClick()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoldNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lift by animateDpAsState(
        targetValue = if (selected) (-10).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "foldNavLift"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .offset(y = lift)
            .height(if (selected) 72.dp else 62.dp)
            .background(if (selected) SomewhereColors.Card else Color.Transparent)
            .drawBehind {
                if (selected) {
                    drawLine(
                        color = SomewhereColors.AccentPurple,
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = SomewhereColors.CardBorder,
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = SomewhereColors.CardBorder,
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .semantics {
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = androidx.compose.ui.semantics.Role.Tab,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) SomewhereColors.TextPrimary else SomewhereColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            color = if (selected) SomewhereColors.TextPrimary else SomewhereColors.TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BottomNavItem(
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(64.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
            .combinedClickable(
                role = androidx.compose.ui.semantics.Role.Tab,
                onClick = {
                    onClick()
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 0.85f,
                            animationSpec = tween(100, easing = FastOutLinearInEasing)
                        )
                        scale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                },
                onLongClick = {
                    onLongClick()
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 0.85f,
                            animationSpec = tween(100, easing = FastOutLinearInEasing)
                        )
                        scale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = description,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
