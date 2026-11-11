package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AriyanDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF330E66),
    onPrimaryContainer = CyanAccent,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0F2659),
    onSecondaryContainer = Color.White,
    tertiary = CyanAccent,
    onTertiary = DeepBlack,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = StatusRed,
    onError = Color.White
)

@Composable
fun AriyanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AriyanDarkColorScheme,
        typography = Typography,
        content = content
    )
}
