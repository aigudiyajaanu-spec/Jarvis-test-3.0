package com.example.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ArcGold
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.ObsidianBlack
import com.example.vision.VisionCameraManager

@Composable
fun VisionHudViewfinder(
    visionManager: VisionCameraManager,
    onClose: () -> Unit,
    onAnalyzeView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        visionManager.bindCamera(lifecycleOwner, previewView)
        onDispose {
            visionManager.unbind()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan_anim")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkNavyCard)
            .border(1.5.dp, CyanGlow.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .testTag("vision_hud_viewfinder")
    ) {
        // Camera Live Surface
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Stark HUD Reticle Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cornerLen = 28.dp.toPx()
            val strokeW = 2.5.dp.toPx()
            val bracketPad = 12.dp.toPx()

            // 4 HUD Corner Brackets
            // Top-Left
            drawLine(CyanGlow, Offset(bracketPad, bracketPad), Offset(bracketPad + cornerLen, bracketPad), strokeW)
            drawLine(CyanGlow, Offset(bracketPad, bracketPad), Offset(bracketPad, bracketPad + cornerLen), strokeW)
            // Top-Right
            drawLine(CyanGlow, Offset(w - bracketPad, bracketPad), Offset(w - bracketPad - cornerLen, bracketPad), strokeW)
            drawLine(CyanGlow, Offset(w - bracketPad, bracketPad), Offset(w - bracketPad, bracketPad + cornerLen), strokeW)
            // Bottom-Left
            drawLine(CyanGlow, Offset(bracketPad, h - bracketPad), Offset(bracketPad + cornerLen, h - bracketPad), strokeW)
            drawLine(CyanGlow, Offset(bracketPad, h - bracketPad), Offset(bracketPad, h - bracketPad - cornerLen), strokeW)
            // Bottom-Right
            drawLine(CyanGlow, Offset(w - bracketPad, h - bracketPad), Offset(w - bracketPad - cornerLen, h - bracketPad), strokeW)
            drawLine(CyanGlow, Offset(w - bracketPad, h - bracketPad), Offset(w - bracketPad, h - bracketPad - cornerLen), strokeW)

            // Center Targeting Crosshair
            val cx = w / 2f
            val cy = h / 2f
            val crossSize = 16.dp.toPx()
            drawCircle(
                color = CyanBright.copy(alpha = 0.5f),
                radius = 24.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(CyanBright.copy(alpha = 0.7f), Offset(cx - crossSize, cy), Offset(cx + crossSize, cy), 1.5.dp.toPx())
            drawLine(CyanBright.copy(alpha = 0.7f), Offset(cx, cy - crossSize), Offset(cx, cy + crossSize), 1.5.dp.toPx())

            // Laser Scan Sweep Line
            val currentLaserY = h * scanLineY
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, CyanBright, ArcGold, CyanBright, Color.Transparent)
                ),
                start = Offset(0f, currentLaserY),
                end = Offset(w, currentLaserY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Top Telemetry Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " OPTICAL SENSOR 01 // 60 FPS",
                    color = CyanBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row {
                IconButton(
                    onClick = { visionManager.toggleCameraLens(lifecycleOwner, previewView) },
                    modifier = Modifier.size(36.dp).testTag("flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = CyanGlow
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp).testTag("close_vision_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Viewfinder",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Action Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ObsidianBlack.copy(alpha = 0.75f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onAnalyzeView,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanGlow,
                    contentColor = ObsidianBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("analyze_viewport_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "  JARVIS, ANALYZE VIEWPORT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
