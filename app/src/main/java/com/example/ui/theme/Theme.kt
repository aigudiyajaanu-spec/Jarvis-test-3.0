package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = ObsidianBlack,
    primaryContainer = CyanDim,
    onPrimaryContainer = IceBlue,
    secondary = ArcGold,
    onSecondary = ObsidianBlack,
    secondaryContainer = DarkNavyCard,
    onSecondaryContainer = ArcGoldBright,
    tertiary = CyanBright,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = IceBlue,
    surface = DarkNavySurface,
    onSurface = IceBlue,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = TextMuted,
    outline = DarkNavyBorder,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}

