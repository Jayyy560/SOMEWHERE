package com.somewhere.app.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors

// The AGSL Shader Code
// It creates a spherical magnification lens effect at 'dropCenter'
// and slightly brightens the pixels under it.
private const val SHADER_CODE = """
    uniform shader composable;
    uniform float2 size;
    uniform float2 dropCenter;
    uniform float dropRadius;

    half4 main(float2 fragCoord) {
        float dist = distance(fragCoord, dropCenter);
        
        // Spherize function: smooth curve
        float influence = smoothstep(dropRadius, 0.0, dist);
        
        if (influence > 0.0) {
            // Direction from center
            float2 dir = (dropCenter - fragCoord); 
            // Normalize direction safely
            float len = max(dist, 0.001);
            dir = dir / len;
            
            // Maximum displacement (bend strength)
            float displacementStrength = 25.0; 
            
            // Spherize formula: bend stronger towards the middle-edges
            float displacement = sin(influence * 3.14159) * displacementStrength; 
            float2 displacedCoord = fragCoord - (dir * displacement);
            
            // Sample the background with displaced coordinates
            half4 color = composable.eval(displacedCoord);
            
            // Add a specular highlight/brightness increase to the text
            // We only brighten the text pixels (color.a > 0.1)
            float highlight = pow(influence, 2.0) * 0.4;
            color.rgb += color.a * highlight;
            
            return color;
        }
        
        return composable.eval(fragCoord);
    }
"""

@Composable
fun LiquidLogo(
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    // 8-second slow float across the screen with a 10-second pause
    val dropXProgress = remember { Animatable(1.3f) }

    LaunchedEffect(Unit) {
        // Wait an initial 3 seconds before the first animation
        kotlinx.coroutines.delay(3000)
        while (true) {
            // Reset to right side
            dropXProgress.snapTo(1.3f)
            // Animate to left side over 8 seconds
            dropXProgress.animateTo(
                targetValue = -0.3f,
                animationSpec = tween(8000, easing = LinearEasing)
            )
            // Pause for 10 seconds before repeating
            kotlinx.coroutines.delay(10000)
        }
    }

    // Calculate dynamic values based on container size
    val dropCenter = Offset(
        x = size.width * dropXProgress.value,
        y = size.height / 2f
    )
    val dropRadius = if (size.height > 0) size.height * 0.4f else 100f

    // Shader setup (API 33+)
    val renderEffect = remember(dropCenter, dropRadius, size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = RuntimeShader(SHADER_CODE)
            shader.setFloatUniform("size", size.width.toFloat(), size.height.toFloat())
            shader.setFloatUniform("dropCenter", dropCenter.x, dropCenter.y)
            shader.setFloatUniform("dropRadius", dropRadius)
            RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .onSizeChanged { size = it },
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: The Text, with the AGSL Shader applied
        Text(
            text = "SOMEWHERE",
            style = MaterialTheme.typography.displayLarge,
            color = SomewhereColors.TextPrimary,
            modifier = Modifier.graphicsLayer {
                this.renderEffect = renderEffect
            }
        )

        // LAYER 2: The Physical Droplet (Blurred, 15-20% Opacity)
        // We draw this on top so it acts as the visible lens casing.
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width > 0 && size.height > 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f), // Core
                            Color.White.copy(alpha = 0.10f), // Mid
                            SomewhereColors.Accent.copy(alpha = 0.05f), // Outer glow
                            Color.Transparent // Edge
                        ),
                        center = dropCenter,
                        radius = dropRadius
                    ),
                    center = dropCenter,
                    radius = dropRadius
                )
            }
        }
    }
}
