package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcGold
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.viewmodel.JarvisHudState
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    state: JarvisHudState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val waveColor = when (state) {
        JarvisHudState.SPEAKING -> CyanBright
        JarvisHudState.LISTENING -> CyanGlow
        JarvisHudState.THINKING -> ArcGold
        else -> CyanGlow.copy(alpha = 0.4f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw 32 vertical equalizer bars with mirrored peaks
        val barCount = 32
        val barSpacing = width / barCount
        val barWidth = barSpacing * 0.55f

        for (i in 0 until barCount) {
            val normalizedIndex = (i - barCount / 2f) / (barCount / 2f)
            val bellCurve = (1f - normalizedIndex * normalizedIndex).coerceIn(0.15f, 1f)

            val waveMod = (sin(i * 0.4 + phase) * 0.5f + 0.5f).toFloat()
            val activeAmp = when (state) {
                JarvisHudState.SPEAKING, JarvisHudState.LISTENING -> (amplitude * 0.8f + waveMod * 0.2f)
                JarvisHudState.THINKING -> (0.25f + waveMod * 0.35f)
                else -> 0.08f
            }

            val barHeight = (activeAmp * bellCurve * height * 0.85f).coerceIn(4.dp.toPx(), height * 0.9f)
            val x = i * barSpacing + (barSpacing - barWidth) / 2f
            val y = midY - barHeight / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0.95f),
                        waveColor.copy(alpha = 0.4f)
                    ),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // Horizontal baseline tech laser line
        drawLine(
            color = waveColor.copy(alpha = 0.3f),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
