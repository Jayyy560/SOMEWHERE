package com.somewhere.app.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors

// The AGSL Shader Code for a wavy, bubbly water blob
private const val SHADER_CODE = """
    uniform shader composable;
    uniform float2 size;
    uniform float2 dropCenter;
    uniform float dropRadius;
    uniform float time;

    half4 main(float2 fragCoord) {
        float2 delta = fragCoord - dropCenter;
        float angle = atan(delta.y, delta.x);
        float baseDist = length(delta);
        
        // Organic liquid blob outline with slowly shifting lobes
        float wave1 = sin(angle * 4.0 + time * 1.2) * 0.05;
        float wave2 = cos(angle * 3.0 - time * 0.8) * 0.07;
        float wave3 = sin(angle * 7.0 + time * 1.5) * 0.02;
        float dynamicRadius = dropRadius * (1.0 + wave1 + wave2 + wave3);
        
        float normalizedDist = baseDist / dynamicRadius;
        
        if (normalizedDist < 1.0) {
            // Base sphere z
            float z = sqrt(max(1.0 - normalizedDist * normalizedDist, 0.0));
            
            // Large flowing water ripples (bump map for normal)
            // fragCoord multipliers create large smooth waves across the drop
            float bumpX = sin(fragCoord.y * 0.02 + time * 0.8) * cos(fragCoord.x * 0.015 + time * 0.5);
            float bumpY = cos(fragCoord.x * 0.02 - time * 0.7) * sin(fragCoord.y * 0.015 - time * 0.6);
            
            // Apply bumps to the normal to make lighting and refraction highly liquidy
            float3 normal = normalize(float3((delta / dynamicRadius) + float2(bumpX, bumpY) * 0.7, z));
            
            // Refraction displacement uses the bumpy normal to violently distort text like real water
            float2 refractionOffset = normal.xy * (dynamicRadius * 0.45); 
            
            float2 displacedCoord = fragCoord - refractionOffset;
            
            // Chromatic aberration (RGB shift) based on the wavy normal
            float shift = dynamicRadius * 0.04 * (1.0 - z);
            half4 bgR = composable.eval(displacedCoord - float2(shift, 0.0));
            half4 bgG = composable.eval(displacedCoord);
            half4 bgB = composable.eval(displacedCoord + float2(shift, 0.0));
            
            // Un-premultiply for color mixing
            half3 rColor = bgR.a > 0.0 ? bgR.rgb / bgR.a : half3(0.0);
            half3 gColor = bgG.a > 0.0 ? bgG.rgb / bgG.a : half3(0.0);
            half3 bColor = bgB.a > 0.0 ? bgB.rgb / bgB.a : half3(0.0);
            
            half3 baseColor = half3(rColor.r, gColor.g, bColor.b);
            
            // Lighting: Highlights will dance on the bumpy surface like water caustics
            float3 lightDir1 = normalize(float3(-1.0, -1.0, 1.5)); 
            float3 lightDir2 = normalize(float3(0.5, -1.0, 0.8));  
            
            float specular1 = pow(max(dot(normal, lightDir1), 0.0), 35.0) * 1.5;
            float specular2 = pow(max(dot(normal, lightDir2), 0.0), 25.0) * 0.7;
            
            // Fresnel effect for edge iridescence
            float fresnel = pow(1.0 - max(dot(normal, float3(0.0, 0.0, 1.0)), 0.0), 3.0);
            half3 rimColor = half3(0.6, 0.8, 1.0) * fresnel; // Liquid cyan/white rim
            
            half3 finalColor = baseColor;
            
            // Add dancing highlights and rim
            finalColor += half3(1.0) * specular1;
            finalColor += half3(0.9, 0.95, 1.0) * specular2;
            finalColor += rimColor;
            
            // Volume darkening on the edge
            float edgeDarkening = smoothstep(0.6, 1.0, normalizedDist);
            finalColor = mix(finalColor, half3(0.0, 0.0, 0.02), edgeDarkening * 0.4);
            
            // Anti-alias edge
            float edgeAlpha = smoothstep(1.0, 0.97, normalizedDist);
            
            half4 blobColor = half4(finalColor, 1.0);
            half4 bg = composable.eval(fragCoord);
            
            return mix(bg, blobColor, edgeAlpha);
        }
        
        return composable.eval(fragCoord);
    }
"""

@Composable
fun LiquidLogo(
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val dropXProgress = remember { Animatable(1.3f) }
    
    // Continuous time for water wobbling
    val infiniteTransition = rememberInfiniteTransition(label = "water_wobble")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        while (true) {
            dropXProgress.snapTo(1.3f)
            dropXProgress.animateTo(
                targetValue = -0.3f,
                animationSpec = tween(8000, easing = LinearEasing)
            )
            kotlinx.coroutines.delay(10000)
        }
    }

    val dropCenter = Offset(
        x = size.width * dropXProgress.value,
        y = size.height / 2f
    )
    val dropRadius = if (size.height > 0) size.height * 0.25f else 60f

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(SHADER_CODE)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .onSizeChanged { size = it }
            .graphicsLayer {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    shader != null &&
                    size.width > 0 &&
                    size.height > 0
                ) {
                    applyRuntimeShader(shader, size, dropCenter, dropRadius, time)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SOMEWHERE",
            style = MaterialTheme.typography.displayLarge,
            color = SomewhereColors.TextPrimary
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun GraphicsLayerScope.applyRuntimeShader(
    shader: RuntimeShader,
    size: IntSize,
    dropCenter: Offset,
    dropRadius: Float,
    time: Float
) {
    shader.setFloatUniform("size", size.width.toFloat(), size.height.toFloat())
    shader.setFloatUniform("dropCenter", dropCenter.x, dropCenter.y)
    shader.setFloatUniform("dropRadius", dropRadius)
    shader.setFloatUniform("time", time)
    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
}
