package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HackerColorScheme = darkColorScheme(
    primary = Color(0xFF3DDC84),
    secondary = Color(0xFF00FF00),
    tertiary = Color(0xFF005500),
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF00FF00),
    onSurface = Color(0xFF00FF00),
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = HackerColorScheme, typography = Typography, content = content)
}
