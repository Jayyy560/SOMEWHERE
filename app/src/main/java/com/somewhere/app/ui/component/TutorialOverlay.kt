package com.somewhere.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.somewhere.app.R
import com.somewhere.app.ui.theme.SomewhereColors
import kotlinx.coroutines.launch

@Composable
fun TutorialOverlay(onComplete: () -> Unit) {
    val pages = listOf(
        TutorialPageData(
            "Welcome to Somewhere",
            "A place to leave a piece of yourself behind. Discover memories left by others, exactly where they happened.",
            R.drawable.tutorial_home
        ),
        TutorialPageData(
            "Look Around & Navigate",
            "Swipe to switch tabs smoothly. The jelly indicator follows your finger. Watch out for glowing drops around you!",
            R.drawable.tutorial_nav
        ),
        TutorialPageData(
            "Capture the Moment",
            "When in the Capture tab, simply tap anywhere to focus, and use your phone's Volume buttons to snap a picture instantly.",
            R.drawable.tutorial_capture
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
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
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = pages[page].imageResId),
                        contentDescription = pages[page].title,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = pages[page].title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = SomewhereColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = pages[page].description,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = SomewhereColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Indicators
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) SomewhereColors.GlowAccent else SomewhereColors.TextMuted
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }

            Button(
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
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SomewhereColors.GlowAccent)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                    color = SomewhereColors.Background,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

data class TutorialPageData(val title: String, val description: String, val imageResId: Int)
