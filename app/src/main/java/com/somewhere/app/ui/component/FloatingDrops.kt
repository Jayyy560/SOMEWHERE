package com.somewhere.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

val SAMPLE_DROPS = listOf(
    "Found the most incredible late-night jazz bar here.",
    "Best tacos in the city, hands down.",
    "The sunset from this rooftop is unreal.",
    "Stumbled upon a hidden street art alley.",
    "Quiet spot to read and forget the world.",
    "The acoustic guitar playing here right now...",
    "Left my favorite book on this bench for the next person.",
    "A tiny bakery with the best croissants.",
    "This park bench has the perfect view.",
    "Underground electronic set going off right now.",
    "Secret garden hideaway.",
    "They serve coffee from a tiny window here.",
    "Someone is playing piano beautifully.",
    "Midnight noodle run, absolutely worth it.",
    "A quiet moment before the city wakes up.",
    "There's a vintage pop-up market hiding here.",
    "The rain against the glass at this cafe.",
    "A perfectly hidden speakeasy.",
    "Street performer playing the saxophone.",
    "Just had the best matcha latte.",
    "Watching the city lights come on.",
    "A little slice of peace in the noise.",
    "Follow the neon sign to the basement.",
    "Incredible skyline view from this parking garage.",
    "Late night diner vibes.",
    "The cherry blossoms are blooming here.",
    "Secret beach access down these stairs.",
    "A tiny bookstore packed to the ceiling.",
    "Listening to the waves crash at midnight.",
    "The best people-watching spot in town."
)

@Composable
fun FloatingDropsAnimation(modifier: Modifier = Modifier) {
    var containerWidth by remember { mutableIntStateOf(0) }
    var currentIndex by remember { mutableIntStateOf(0) }
    
    val offsetX = remember { Animatable(2000f) }

    LaunchedEffect(containerWidth) {
        if (containerWidth > 0) {
            while (true) {
                // Start completely off-screen to the right
                val startX = containerWidth.toFloat() + 200f
                val centerX = 0f
                val endX = -containerWidth.toFloat() - 800f
                
                offsetX.snapTo(startX)
                
                // Slide in quickly but smoothly
                offsetX.animateTo(
                    targetValue = centerX,
                    animationSpec = tween(
                        durationMillis = 2500,
                        easing = EaseOutCubic
                    )
                )
                
                // Hold in the center
                delay(3000)
                
                // Slide out to the left
                offsetX.animateTo(
                    targetValue = endX,
                    animationSpec = tween(
                        durationMillis = 2000,
                        easing = EaseInCubic
                    )
                )
                
                // Move to next drop
                currentIndex = (currentIndex + 1) % SAMPLE_DROPS.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.Center
    ) {
        if (containerWidth > 0) {
            Box(
                modifier = Modifier.offset(x = offsetX.value.dp)
            ) {
                key(currentIndex) {
                    GlassDropBubble(text = SAMPLE_DROPS[currentIndex])
                }
            }
        }
    }
}

class WavyPillShape(private val progress: Float, private val amplitudeMultiplier: Float = 1f) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val width = size.width
        val height = size.height
        val radius = minOf(width, height) / 2f

        // The maximum amount the wave can bulge or pinch (reduced for subtle effect)
        val waveAmplitude = height * 0.08f * amplitudeMultiplier
        // Even more subtle wave for the sides
        val sideWaveAmplitude = height * 0.02f * amplitudeMultiplier

        val phase1 = progress
        val phase2 = progress + kotlin.math.PI.toFloat() * 0.7f
        val rightPhase = progress + kotlin.math.PI.toFloat() * 1.3f
        val leftPhase = progress + kotlin.math.PI.toFloat() * 2.1f

        // Top edge control points
        val topCtrlY1 = waveAmplitude * kotlin.math.sin(phase1)
        val topCtrlY2 = waveAmplitude * kotlin.math.sin(phase2)

        // Bottom edge control points
        val bottomCtrlY1 = height + waveAmplitude * kotlin.math.cos(phase1)
        val bottomCtrlY2 = height + waveAmplitude * kotlin.math.cos(phase2)
        
        // Left and Right bulge offsets
        val rightBulge = sideWaveAmplitude * kotlin.math.sin(rightPhase)
        val leftBulge = sideWaveAmplitude * kotlin.math.cos(leftPhase)
        
        // The horizontal offset for the control points to approximate a soft pill end
        val bezierOffset = radius * 1.33f

        // Start at top-left of the straight section
        path.moveTo(radius, 0f)

        // Top wavy edge
        path.cubicTo(
            width * 0.33f, topCtrlY1,
            width * 0.66f, topCtrlY2,
            width - radius, 0f
        )

        // Right wavy edge (subtle)
        path.cubicTo(
            width - radius + bezierOffset + rightBulge, 0f,
            width - radius + bezierOffset + rightBulge, height,
            width - radius, height
        )

        // Bottom wavy edge (moving right to left)
        path.cubicTo(
            width * 0.66f, bottomCtrlY1,
            width * 0.33f, bottomCtrlY2,
            radius, height
        )

        // Left wavy edge (subtle)
        path.cubicTo(
            radius - bezierOffset - leftBulge, height,
            radius - bezierOffset - leftBulge, 0f,
            radius, 0f
        )

        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun GlassDropBubble(text: String) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animate from 0 to 2PI for the sine waves in WavyPillShape
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = (2 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), 
            repeatMode = RepeatMode.Restart
        )
    )

    var isAgitated by remember { mutableStateOf(false) }

    LaunchedEffect(isAgitated) {
        if (isAgitated) {
            kotlinx.coroutines.delay(500)
            isAgitated = false
        }
    }

    // Animation specsd, we multiply the wave amplitude and speed up the breathing
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isAgitated) 2.5f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 150f),
        label = "AmplitudeBounce"
    )
    
    val bubblyShape = WavyPillShape(waveProgress, amplitudeMultiplier)

    // Breathing scale for gelatinous movement (we can also speed this up if agitated, but keeping it simple)
    val scaleX by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(if (isAgitated) 700 else 1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 1.02f, targetValue = 0.98f,
        animationSpec = infiniteRepeatable(tween(if (isAgitated) 700 else 1500, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // 3D Iridescent Liquid Glass implementation
    Box(
        modifier = Modifier
            .padding(16.dp)
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isAgitated = true
                    }
                )
            }
            .shadow(
                elevation = 24.dp,
                shape = bubblyShape,
                ambientColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                spotColor = Color(0xFFFF00FF).copy(alpha = 0.3f)
            )
            .clip(bubblyShape)
            // Base subtle darkness to contrast the reflections
            .background(Color.White.copy(alpha = 0.02f))
            // Liquid fluid blobs (Cyan)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF80D8FF).copy(alpha = 0.2f), 
                        Color.Transparent
                    ),
                    center = Offset(100f, 50f),
                    radius = 300f
                )
            )
            // Liquid fluid blobs (Magenta/Purple)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEA80FC).copy(alpha = 0.15f), 
                        Color.Transparent
                    ),
                    center = Offset(500f, 150f),
                    radius = 400f
                )
            )
            // Top and bottom internal glass curve reflections
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f), // Top highlight
                        Color.Transparent,               // Deep hollow center
                        Color.White.copy(alpha = 0.1f),  // Bottom reflection
                        Color.White.copy(alpha = 0.4f)   // Bottom edge rim light
                    )
                )
            )
            // Outer Iridescent Thick Glass Rim
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,                             // Top-left intense glare
                        Color(0xFF80D8FF).copy(alpha = 0.6f),    // Cyan dispersion
                        Color.White.copy(alpha = 0.1f),          // Weak sides
                        Color(0xFFEA80FC).copy(alpha = 0.5f),    // Magenta dispersion
                        Color.White                              // Bottom-right intense glare
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = bubblyShape
            )
            // Inner thickness bevel
            .padding(3.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f), 
                        Color.Transparent, 
                        Color.White.copy(alpha = 0.2f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = bubblyShape
            )
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // The text inside the bubble
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Normal,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 4f),
                    blurRadius = 8f
                )
            ),
            maxLines = 1
        )
        
        // Add the intense starburst glare on the bottom right corner
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 10.dp, y = 5.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            radius = 30f
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}
