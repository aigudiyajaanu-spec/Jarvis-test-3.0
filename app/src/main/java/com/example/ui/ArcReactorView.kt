package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcGold
import com.example.ui.theme.ArcGoldBright
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanDim
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkNavyCardElevated
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.JarvisHudState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorView(
    state: JarvisHudState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_anim")

    // Rotation speed adapts smoothly based on state
    val outerDuration = when (state) {
        JarvisHudState.THINKING -> 3000
        JarvisHudState.LISTENING -> 8000
        JarvisHudState.SPEAKING -> 5000
        else -> 14000
    }
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = outerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_outer"
    )

    val midDuration = when (state) {
        JarvisHudState.THINKING -> 2000
        JarvisHudState.LISTENING -> 6000
        JarvisHudState.SPEAKING -> 4000
        else -> 10000
    }
    val rotationMid by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = midDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_mid"
    )

    val innerDuration = when (state) {
        JarvisHudState.THINKING -> 1500
        JarvisHudState.LISTENING -> 4500
        JarvisHudState.SPEAKING -> 3000
        else -> 7500
    }
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = innerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_inner"
    )

    val pulseDuration = when (state) {
        JarvisHudState.LISTENING -> 800
        JarvisHudState.SPEAKING -> 600
        JarvisHudState.THINKING -> 500
        else -> 1300
    }
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val primaryColor = when (state) {
        JarvisHudState.IDLE -> CyanGlow
        JarvisHudState.LISTENING -> CyanBright
        JarvisHudState.THINKING -> ArcGoldBright
        JarvisHudState.SPEAKING -> CyanCore
        JarvisHudState.ERROR -> AlertRed
    }

    val secondaryColor = when (state) {
        JarvisHudState.IDLE -> CyanDim
        JarvisHudState.LISTENING -> CyanGlow
        JarvisHudState.THINKING -> ArcGold
        JarvisHudState.SPEAKING -> ArcGoldBright
        JarvisHudState.ERROR -> AlertRed
    }

    Box(
        modifier = modifier
            .size(250.dp)
            .testTag("arc_reactor_button")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f - 14.dp.toPx()
            val dynamicScale = (1f + (amplitude * 0.42f) * pulseGlow).coerceIn(0.92f, 1.55f)

            // 1. Ambient Radial Plasma Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = (0.28f * dynamicScale).coerceIn(0.12f, 0.7f)),
                        secondaryColor.copy(alpha = (0.12f * dynamicScale).coerceIn(0.04f, 0.35f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius * 1.35f,
                center = center
            )

            // 2. Outermost Precision Optical HUD Ring
            rotate(rotationOuter, pivot = center) {
                val outerRadius = baseRadius
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.45f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 10f, 6f, 10f), 0f)
                    )
                )

                // 12 Outer Micro Nodes
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30).toDouble())
                    val nodePos = Offset(
                        (center.x + outerRadius * cos(angle)).toFloat(),
                        (center.y + outerRadius * sin(angle)).toFloat()
                    )
                    drawCircle(
                        color = if (i % 3 == 0) primaryColor else secondaryColor.copy(alpha = 0.6f),
                        radius = if (i % 3 == 0) 3.5.dp.toPx() else 1.8.dp.toPx(),
                        center = nodePos
                    )
                }
            }

            // 3. Middle Counter-Rotating Arc Stator
            rotate(rotationMid, pivot = center) {
                val midRadius = baseRadius * 0.82f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.6f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )

                // 10 Stark Fusion Inductor Bars
                for (i in 0 until 10) {
                    val angle = Math.toRadians((i * 36).toDouble())
                    val p1 = Offset(
                        (center.x + (midRadius - 7.dp.toPx()) * cos(angle)).toFloat(),
                        (center.y + (midRadius - 7.dp.toPx()) * sin(angle)).toFloat()
                    )
                    val p2 = Offset(
                        (center.x + (midRadius + 7.dp.toPx()) * cos(angle)).toFloat(),
                        (center.y + (midRadius + 7.dp.toPx()) * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = ArcGoldBright.copy(alpha = 0.85f),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Quantum Core Ring with Live Amplitude Arcs
            rotate(rotationInner, pivot = center) {
                val innerRadius = baseRadius * 0.60f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.75f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                val waveCount = 8
                for (w in 0 until waveCount) {
                    val arcAngle = 360f / waveCount
                    val sweep = if (state == JarvisHudState.LISTENING || state == JarvisHudState.SPEAKING) {
                        (arcAngle * 0.65f * dynamicScale).coerceIn(8f, arcAngle * 0.9f)
                    } else {
                        arcAngle * 0.35f
                    }
                    drawArc(
                        color = primaryColor,
                        startAngle = (w * arcAngle),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = Size(innerRadius * 2, innerRadius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 5. Solid Deep Core Base
            val coreRadius = baseRadius * 0.40f * dynamicScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DarkNavyCardElevated,
                        DarkNavySurface,
                        ObsidianBlack
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 6. Core Radiant Energy Center
            drawCircle(
                color = primaryColor.copy(alpha = 0.95f),
                radius = coreRadius * 0.52f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = coreRadius * 0.28f,
                center = center
            )
        }

        // Center Icon based on state
        val iconSize = 36.dp
        when (state) {
            JarvisHudState.IDLE -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Tap to speak",
                    tint = CyanBright,
                    modifier = Modifier.size(iconSize)
                )
            }
            JarvisHudState.LISTENING -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Listening",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(iconSize)
                )
            }
            JarvisHudState.THINKING -> {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Thinking",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(iconSize)
                )
            }
            JarvisHudState.SPEAKING -> {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Speaking",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(iconSize)
                )
            }
            JarvisHudState.ERROR -> {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = "Error state",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

