package com.jmalinen.blackjack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackjackColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    secondary = FeltGreenLight,
    onSecondary = Color.White,
    background = FeltGreen,
    onBackground = Color.White,
    surface = FeltGreenDark,
    onSurface = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun BlackjackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackjackColorScheme,
        typography = Typography,
        content = content
    )
}
