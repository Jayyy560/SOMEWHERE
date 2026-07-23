package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.ui.theme.SomewhereColors
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingBottomNav(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (Int) -> Unit,
    onProfileLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Icons.Default.DirectionsWalk to "Trips",
        Icons.Default.Home to "Home",
        Icons.Default.Add to "Create",
        Icons.Default.Person to "Profile"
    )

    Box(
        modifier = modifier
            .padding(bottom = 32.dp)
            .height(76.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.72f))
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
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (icon, desc) ->
                BottomNavItem(
                    icon = icon,
                    description = desc,
                    isSelected = pagerState.currentPage == index,
                    onClick = { onTabSelected(index) },
                    onLongClick = {
                        if (index == 3) {
                            onProfileLongClick()
                        }
                    }
                )
            }
        }
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
            .width(72.dp)
            .height(60.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    com.somewhere.app.ui.theme.LocalAmbientColors.current.glowPrimary.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.28f) else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            )
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
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
                maxLines = 1
            )
        }
    }
}
