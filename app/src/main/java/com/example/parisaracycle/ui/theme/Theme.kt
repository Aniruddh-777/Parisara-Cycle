package com.example.parisaracycle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    secondary = Color(0xFF006D77),
    tertiary = Color(0xFF8B5E34),
    background = Color(0xFFF7F9F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4E9DE),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD68F),
    secondary = Color(0xFF78D5DF),
    tertiary = Color(0xFFE3BF9C),
    background = Color(0xFF10150F),
    surface = Color(0xFF181D16),
    surfaceVariant = Color(0xFF40483C),
    error = Color(0xFFFFB4AB)
)

@Composable
fun ParisaraCycleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
