package com.example.privacycenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonGreen,
    tertiary = NeonPurple,

    background = DarkBg,
    surface = DarkCard,

    error = DangerRed,

    onPrimary = Color.Black,
    onBackground = Color.White
)

@Composable
fun PrivacyCenterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}