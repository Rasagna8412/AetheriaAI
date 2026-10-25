package com.aetheria.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RavenColorScheme = darkColorScheme(
    primary = RavenViolet,
    onPrimary = RavenTextPrimary,
    primaryContainer = RavenAmethyst.copy(alpha = 0.2f),
    onPrimaryContainer = RavenTextPrimary,
    secondary = RavenCyan,
    onSecondary = RavenVoid,
    secondaryContainer = RavenCyan.copy(alpha = 0.2f),
    tertiary = RavenEmerald,
    background = RavenVoid,
    surface = RavenSurface,
    surfaceVariant = RavenCard,
    onBackground = RavenTextPrimary,
    onSurface = RavenTextPrimary,
    onSurfaceVariant = RavenTextSecondary,
    outline = RavenBorder
)

@Composable
fun AetheriaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RavenColorScheme,
        typography = Typography,
        content = content
    )
}
