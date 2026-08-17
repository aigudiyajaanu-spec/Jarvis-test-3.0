package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisSettingsSheet(
    viewModel: JarvisViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apiKey by viewModel.preferences.apiKey.collectAsState()
    val currentVoice by viewModel.preferences.voiceName.collectAsState()
    val currentThinking by viewModel.preferences.thinkingLevel.collectAsState()
    val isVisionDefault by viewModel.preferences.isVisionEnabled.collectAsState()
    val validationStatus by viewModel.keyValidationStatus.collectAsState()
    val isValidating by viewModel.isValidatingKey.collectAsState()

    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }

    val voices = listOf(
        Pair("Orus", "Refined, calm British male (Standard JARVIS)"),
        Pair("Charon", "Deep, authoritative tactical male voice"),
        Pair("Fenrir", "Crisp, commanding resonance"),
        Pair("Puck", "Agile, energetic tone"),
        Pair("Aoede", "Clear, melodic cadence")
    )

    val thinkingLevels = listOf("minimal", "low", "medium", "high")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkNavyBg,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("jarvis_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "JARVIS PROTOCOL CONFIGURATION",
                    color = CyanBright,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Section 1: API Key
            Text(
                text = "GEMINI NEURAL LINK // API KEY",
                color = ArcGoldBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Gemini API Key", color = TextMuted) },
                placeholder = { Text("AIzaSy...", color = TextMuted.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = CyanGlow
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanGlow,
                    unfocusedBorderColor = DarkNavyBorder,
                    focusedTextColor = IceBlue,
                    unfocusedTextColor = IceBlue,
                    focusedContainerColor = DarkNavySurface,
                    unfocusedContainerColor = DarkNavySurface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_key_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { viewModel.validateAndSaveKey(keyInput) },
                    enabled = !isValidating && keyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGlow,
                        contentColor = ObsidianBlack,
                        disabledContainerColor = CyanDim.copy(alpha = 0.4f),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("validate_save_key_btn")
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            color = ObsidianBlack,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isValidating) "VERIFYING LINK..." else "TEST & SAVE PROTOCOL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            validationStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                val isSuccess = status.contains("Successful", ignoreCase = true) || status.contains("Online", ignoreCase = true)
                Text(
                    text = status,
                    color = if (isSuccess) SuccessGreen else AlertRed,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Voice Personality Picker
            Text(
                text = "NEURAL VOICE SYNTHESIS // PREBUILT VOICE",
                color = ArcGoldBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voices.forEach { (voiceId, desc) ->
                    val isSelected = currentVoice.equals(voiceId, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) DarkNavyCard else DarkNavySurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyanGlow else DarkNavyBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.setVoice(voiceId) }
                            .padding(12.dp)
                            .testTag("voice_option_$voiceId")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voiceId.uppercase(),
                                    color = if (isSelected) CyanBright else IceBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = desc,
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = CyanGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Latency & Thinking Level
            Text(
                text = "NEURAL THINKING LEVEL // LATENCY TUNING",
                color = ArcGoldBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                thinkingLevels.forEach { level ->
                    val isSelected = currentThinking == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyanGlow else DarkNavySurface)
                            .border(
                                1.dp,
                                if (isSelected) CyanBright else DarkNavyBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setThinking(level) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level.uppercase(),
                            color = if (isSelected) ObsidianBlack else IceBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Optical Viewfinder Default
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkNavySurface)
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STARTUP OPTICAL VIEWPORT",
                        color = IceBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Enable camera HUD sensor on launch",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isVisionDefault,
                    onCheckedChange = { viewModel.preferences.setVisionDefault(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBlack,
                        checkedTrackColor = CyanGlow,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkNavyCard
                    ),
                    modifier = Modifier.testTag("vision_default_switch")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
