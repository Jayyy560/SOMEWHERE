package com.somewhere.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.ui.theme.SomewhereColors
import kotlinx.coroutines.launch

@Composable
fun TutorialOverlay(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = listOf(
        TutorialPageData(
            eyebrow = "LEAVE SOMETHING HERE",
            title = "A place can hold a story",
            description = "Take a photo, add a message, and leave the Drop at your current location. People who come nearby can discover it.",
            kind = TutorialStory.LEAVE
        ),
        TutorialPageData(
            eyebrow = "FIND WHAT OTHERS LEFT",
            title = "Get close to open it",
            description = "Explore shows Drops within 500 m. Walk within about 30 m to unlock one. Distances are estimates and can shift with GPS.",
            kind = TutorialStory.DISCOVER
        ),
        TutorialPageData(
            eyebrow = "CHOOSE WHAT HAPPENS NEXT",
            title = "Not every Drop stays",
            description = "Permanent Drops stay where they were left. Moments expire. Hitchhikers can be picked up and released somewhere new.",
            kind = TutorialStory.JOURNEY
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val ambient = com.somewhere.app.ui.theme.LocalAmbientColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background.copy(alpha = 0.95f))
    ) {
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Skip", color = SomewhereColors.TextPrimary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 44.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pages[page].eyebrow,
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = ambient.pulseColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TutorialStoryArtwork(
                        kind = pages[page].kind,
                        accent = ambient.pulseColor
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = pages[page].title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            lineHeight = 34.sp
                        ),
                        color = SomewhereColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pages[page].description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = SomewhereColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 360.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { iteration ->
                    val selected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) ambient.pulseColor
                                else SomewhereColors.TextMuted.copy(alpha = 0.45f)
                            )
                            .size(
                                width = if (selected) 22.dp else 6.dp,
                                height = 6.dp
                            )
                    )
                }
            }

            SomewhereButton(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                minWidth = 0.dp,
                height = 56.dp,
                text = if (pagerState.currentPage < pages.size - 1) {
                        "Continue"
                    } else {
                        "Enter Somewhere"
                    }
            )
        }
    }
}

private enum class TutorialStory {
    LEAVE,
    DISCOVER,
    JOURNEY
}

private data class TutorialPageData(
    val eyebrow: String,
    val title: String,
    val description: String,
    val kind: TutorialStory
)

@Composable
private fun TutorialStoryArtwork(
    kind: TutorialStory,
    accent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.025f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        when (kind) {
            TutorialStory.LEAVE -> LeaveArtwork(accent)
            TutorialStory.DISCOVER -> DiscoverArtwork(accent)
            TutorialStory.JOURNEY -> JourneyArtwork(accent)
        }
    }
}

@Composable
private fun BoxScope.LeaveArtwork(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.54f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.46f
            ),
            radius = size.minDimension * 0.46f,
            center = center
        )
        drawLine(
            color = accent.copy(alpha = 0.30f),
            start = Offset(center.x, center.y + 42.dp.toPx()),
            end = Offset(center.x, size.height - 26.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(
            color = accent.copy(alpha = 0.14f),
            radius = 27.dp.toPx(),
            center = Offset(center.x, size.height - 25.dp.toPx())
        )
        drawCircle(
            color = accent.copy(alpha = 0.55f),
            radius = 4.dp.toPx(),
            center = Offset(center.x, size.height - 25.dp.toPx())
        )
    }

    GlassStoryOrb(
        icon = Icons.Default.CameraAlt,
        contentDescription = "Photo attached to a location",
        accent = accent,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = (-4).dp)
    )

    Icon(
        imageVector = Icons.Default.LocationOn,
        contentDescription = null,
        tint = accent.copy(alpha = 0.85f),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp)
            .size(25.dp)
    )
}

@Composable
private fun BoxScope.DiscoverArtwork(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        listOf(37.dp, 76.dp, 110.dp).forEachIndexed { index, radius ->
            drawCircle(
                color = accent.copy(alpha = 0.28f - index * 0.07f),
                radius = radius.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = if (index == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            )
        }
        drawCircle(
            color = accent.copy(alpha = 0.07f),
            radius = 37.dp.toPx(),
            center = center
        )
        drawCircle(accent.copy(alpha = 0.8f), 5.dp.toPx(), center)
        drawCircle(
            accent.copy(alpha = 0.7f),
            7.dp.toPx(),
            Offset(center.x + 72.dp.toPx(), center.y - 38.dp.toPx())
        )
        drawCircle(
            accent.copy(alpha = 0.45f),
            5.dp.toPx(),
            Offset(center.x - 91.dp.toPx(), center.y + 35.dp.toPx())
        )
    }

    Icon(
        imageVector = Icons.Default.LockOpen,
        contentDescription = "Nearby Drop unlocked",
        tint = SomewhereColors.TextPrimary,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = (-2).dp)
            .size(28.dp)
    )

    DistanceLabel(
        text = "30 m · opens",
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 29.dp),
        color = SomewhereColors.TextPrimary
    )
    DistanceLabel(
        text = "500 m · visible",
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 24.dp),
        color = SomewhereColors.TextSecondary
    )
}

@Composable
private fun BoxScope.JourneyArtwork(accent: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val y = size.height * 0.48f
        val startX = 52.dp.toPx()
        val endX = size.width - 52.dp.toPx()
        val step = (endX - startX) / 2f

        drawLine(
            color = accent.copy(alpha = 0.28f),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 1.dp.toPx()
        )
        repeat(3) { index ->
            val point = Offset(startX + step * index, y)
            drawCircle(
                color = accent.copy(alpha = if (index == 2) 0.18f else 0.08f),
                radius = 31.dp.toPx(),
                center = point
            )
            drawCircle(
                color = accent.copy(alpha = 0.8f),
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }

    JourneyStop(
        icon = Icons.Default.LocationOn,
        label = "Permanent",
        accent = accent,
        modifier = Modifier.align(Alignment.CenterStart).padding(start = 28.dp)
    )
    JourneyStop(
        icon = Icons.Default.Schedule,
        label = "Moment",
        accent = accent,
        modifier = Modifier.align(Alignment.Center)
    )
    JourneyStop(
        icon = Icons.Default.DirectionsWalk,
        label = "Hitchhiker",
        accent = accent,
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp)
    )
}

@Composable
private fun GlassStoryOrb(
    icon: ImageVector,
    contentDescription: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(94.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        accent.copy(alpha = 0.13f),
                        Color.Black.copy(alpha = 0.55f)
                    ),
                    center = Offset(28f, 20f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = SomewhereColors.TextPrimary,
            modifier = Modifier.size(31.dp)
        )
    }
}

@Composable
private fun DistanceLabel(
    text: String,
    modifier: Modifier,
    color: Color
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Medium
        )
    )
}

@Composable
private fun JourneyStop(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.48f))
                .border(1.dp, accent.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SomewhereColors.TextPrimary,
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            color = SomewhereColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}
