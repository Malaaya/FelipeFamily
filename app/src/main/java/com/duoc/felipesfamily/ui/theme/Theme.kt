package com.duoc.felipesfamily.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    secondary = AccentOrange,
    onSecondary = OnAccent,
    background = Color(0xFFF0F4F0),
    onBackground = Color(0xFF14221A),
    surface = Color.White,
    onSurface = Color(0xFF14221A),
    onSurfaceVariant = Color(0xFF5D6B62),
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreenLight,
    onPrimary = Color(0xFF0A2219),
    secondary = AccentOrange,
    onSecondary = OnAccent,
    background = Color(0xFF0D1511),
    onBackground = Color(0xFFE8EFE9),
    surface = Color(0xFF16211A),
    onSurface = Color(0xFFE8EFE9),
    onSurfaceVariant = Color(0xFF9FB0A4),
    error = Color(0xFFFF6B6B)
)

@Composable
fun FelipesFamilyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
