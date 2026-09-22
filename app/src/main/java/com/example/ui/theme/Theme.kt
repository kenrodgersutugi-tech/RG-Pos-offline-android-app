package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF001B24),
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = EmeraldSuccess,
    onSecondary = Color(0xFF002114),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = VioletAccent,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate200,
    outline = Slate700,
    error = RoseError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark-first design for RG POS
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
