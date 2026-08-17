package com.example.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcGold
import com.example.ui.theme.ArcGoldBright
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDim
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.viewmodel.JarvisHudState
import com.example.viewmodel.JarvisViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun JarvisHUDMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val amplitude by viewModel.liveAmplitude.collectAsStateWithLifecycle()
    val currentSubtitle by viewModel.currentSubtitle.collectAsStateWithLifecycle()
    val isVisionMode by viewModel.isVisionMode.collectAsStateWithLifecycle()
    val telemetryLogs by viewModel.telemetryLogs.collectAsStateWithLifecycle()
    val apiKey by viewModel.preferences.apiKey.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTerminalDrawer by remember { mutableStateOf(false) }

    // Permissions for Mic & Camera
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DarkNavySurface,
                            DarkNavyBg,
                            ObsidianBlack
                        ),
                        radius = 1800f
                    )
                )
                .drawBehind {
                    // Subtle Stark Tactical Holographic Grid Lines
                    val gridSpacing = 52.dp.toPx()
                    val gridColor = CyanDim.copy(alpha = 0.04f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. TOP HUD TELEMETRY BAR
                TopHudBar(
                    hudState = hudState,
                    isVisionActive = isVisionMode,
                    onToggleVision = { viewModel.toggleVision() },
                    onOpenTerminal = { showTerminalDrawer = true },
                    onOpenSettings = { showSettingsSheet = true }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. OPTICAL VIEWFINDER (IF ACTIVE)
                AnimatedVisibility(
                    visible = isVisionMode,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    VisionHudViewfinder(
                        visionManager = viewModel.visionManager,
                        onClose = { viewModel.toggleVision() },
                        onAnalyzeView = { viewModel.captureVisionAndQuery() },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 3. CENTER STAGE: ARC REACTOR CORE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ArcReactorView(
                            state = hudState,
                            amplitude = amplitude,
                            onClick = {
                                if (!permissionsState.allPermissionsGranted) {
                                    permissionsState.launchMultiplePermissionRequest()
                                } else {
                                    viewModel.onReactorClick()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // State Badge
                        StateBadge(state = hudState)
                    }
                }

                // 4. LIVE AUDIO SPECTRUM EQUALIZER
                WaveformVisualizer(
                    state = hudState,
                    amplitude = amplitude,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // 5. LIVE SUBTITLES & PROTOCOL READOUT
                SubtitleHudCard(
                    subtitle = currentSubtitle,
                    state = hudState,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 6. QUICK PROTOCOL CHIPS
                QuickActionChips(
                    onQuery = { query -> viewModel.sendUserQuery(query) },
                    onAnalyzeVision = { viewModel.captureVisionAndQuery() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 7. HUD COMMAND INPUT ROW (VOICE & TEXT COMMANDS)
                HudCommandInputRow(
                    hudState = hudState,
                    onSendCommand = { command -> viewModel.sendUserQuery(command) },
                    onMicClick = {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else {
                            viewModel.onReactorClick()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    // Settings Modal Bottom Sheet
    if (showSettingsSheet) {
        JarvisSettingsSheet(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Terminal Drawer Modal Bottom Sheet
    if (showTerminalDrawer) {
        JarvisTerminalDrawer(
            logs = telemetryLogs,
            onDismiss = { showTerminalDrawer = false },
            onClearLogs = { viewModel.clearLogs() },
            onClearHistory = { viewModel.clearHistory() }
        )
    }
}

@Composable
fun TopHudBar(
    hudState: JarvisHudState,
    isVisionActive: Boolean,
    onToggleVision: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkNavySurface)
                .border(1.dp, CyanDim.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hudState == JarvisHudState.ERROR) AlertRed else SuccessGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "JARVIS // ONLINE",
                color = CyanBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Action Icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vision Toggle Button
            IconButton(
                onClick = onToggleVision,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isVisionActive) CyanGlow.copy(alpha = 0.2f) else DarkNavySurface)
                    .border(1.dp, if (isVisionActive) CyanGlow else DarkNavyBorder, CircleShape)
                    .testTag("toggle_vision_btn")
            ) {
                Icon(
                    imageVector = if (isVisionActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Toggle Vision HUD",
                    tint = if (isVisionActive) CyanBright else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Terminal Logs Button
            IconButton(
                onClick = onOpenTerminal,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkNavySurface)
                    .border(1.dp, DarkNavyBorder, CircleShape)
                    .testTag("open_terminal_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Diagnostic Terminal",
                    tint = CyanGlow,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkNavySurface)
                    .border(1.dp, DarkNavyBorder, CircleShape)
                    .testTag("open_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "JARVIS Settings",
                    tint = ArcGoldBright,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StateBadge(state: JarvisHudState) {
    val (text, color) = when (state) {
        JarvisHudState.IDLE -> Pair("STANDBY // TAP REACTOR TO ENGAGE", CyanGlow)
        JarvisHudState.LISTENING -> Pair("ACOUSTIC RECEIVER ACTIVE // LISTENING...", CyanBright)
        JarvisHudState.THINKING -> Pair("NEURAL CORE PROCESSING PROTOCOLS...", ArcGoldBright)
        JarvisHudState.SPEAKING -> Pair("TRANSMITTING VOICE // TAP TO INTERRUPT", IceBlue)
        JarvisHudState.ERROR -> Pair("SYSTEM ANOMALY DETECTED", AlertRed)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavySurface.copy(alpha = 0.8f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SubtitleHudCard(
    subtitle: String,
    state: JarvisHudState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subtitle_hud_card"),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavySurface.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkNavyBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (state == JarvisHudState.SPEAKING) CyanBright else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = subtitle,
                color = IceBlue,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionChips(
    onQuery: (String) -> Unit,
    onAnalyzeVision: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickChip(
            icon = Icons.Default.Info,
            label = "HELLO JARVIS",
            onClick = { onQuery("Hello JARVIS, give me an introduction and status check.") }
        )
        QuickChip(
            icon = Icons.Default.Security,
            label = "STATUS REPORT",
            onClick = { onQuery("JARVIS, give me a full system and battery diagnostics status report.") }
        )
        QuickChip(
            icon = Icons.Default.CameraAlt,
            label = "ANALYZE VIEW",
            onClick = onAnalyzeVision
        )
        QuickChip(
            icon = Icons.Default.Schedule,
            label = "SYSTEM TIME",
            onClick = { onQuery("JARVIS, what is the exact current time and date?") }
        )
        QuickChip(
            icon = Icons.Default.FlashlightOn,
            label = "TOGGLE TORCH",
            onClick = { onQuery("JARVIS, toggle the flashlight torch.") }
        )
        QuickChip(
            icon = Icons.Default.Language,
            label = "OPEN GITHUB",
            onClick = { onQuery("JARVIS, open https://github.com in the browser.") }
        )
    }
}

@Composable
fun HudCommandInputRow(
    hudState: JarvisHudState,
    onSendCommand: (String) -> Unit,
    onMicClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkNavySurface)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mic / Action Button
        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (hudState == JarvisHudState.LISTENING) CyanGlow.copy(alpha = 0.3f)
                    else DarkNavyCard
                )
                .border(
                    1.dp,
                    if (hudState == JarvisHudState.LISTENING) CyanBright else CyanDim.copy(alpha = 0.4f),
                    CircleShape
                )
                .testTag("hud_mic_btn")
        ) {
            Icon(
                imageVector = when (hudState) {
                    JarvisHudState.LISTENING -> Icons.Default.Mic
                    JarvisHudState.SPEAKING -> Icons.Default.Stop
                    else -> Icons.Default.Mic
                },
                contentDescription = "Voice Input",
                tint = if (hudState == JarvisHudState.LISTENING) CyanBright else IceBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Command Text Field
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = {
                Text(
                    text = if (hudState == JarvisHudState.LISTENING) "Listening to speech..." else "Command JARVIS or ask anything...",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            modifier = Modifier
                .weight(1f)
                .testTag("command_input_field"),
            textStyle = TextStyle(
                color = IceBlue,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    val query = text.trim()
                    if (query.isNotBlank()) {
                        keyboardController?.hide()
                        text = ""
                        onSendCommand(query)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = CyanBright
            )
        )

        // Send Button
        IconButton(
            onClick = {
                val query = text.trim()
                if (query.isNotBlank()) {
                    keyboardController?.hide()
                    text = ""
                    onSendCommand(query)
                }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (text.isNotBlank()) CyanGlow.copy(alpha = 0.2f) else Color.Transparent)
                .testTag("command_send_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Command",
                tint = if (text.isNotBlank()) CyanBright else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun QuickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkNavyCard)
            .border(1.dp, CyanDim.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanGlow,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = IceBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
