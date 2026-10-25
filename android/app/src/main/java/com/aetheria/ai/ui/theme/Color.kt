package com.aetheria.ai.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Raven Dark Theme
val RavenVoid = Color(0xFF080A0F)        // Deepest OLED black
val RavenSurface = Color(0xFF0F131D)     // Surface background
val RavenCard = Color(0xFF171D2B)        // Card background
val RavenCardElevated = Color(0xFF1F273A)// Elevated card / input
val RavenBorder = Color(0xFF2B364E)      // Subtle glass border
val RavenBorderLight = Color(0x33FFFFFF) // 20% white border for glassmorphism

// Neon Accents
val RavenViolet = Color(0xFF8B5CF6)      // Primary brand violet
val RavenAmethyst = Color(0xFFA855F7)    // Vibrant amethyst
val RavenCyan = Color(0xFF06B6D4)        // Electric cyan
val RavenSky = Color(0xFF38BDF8)         // Bright sky blue
val RavenEmerald = Color(0xFF10B981)     // Online status / success
val RavenAmber = Color(0xFFF59E0B)       // Offline / warning
val RavenRose = Color(0xFFF43F5E)        // Uncensored / delete

// Typography Colors
val RavenTextPrimary = Color(0xFFF8FAFC)
val RavenTextSecondary = Color(0xFF94A3B8)
val RavenTextMuted = Color(0xFF64748B)

// Gradients
val RavenBrandGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))
)

val RavenUserBubbleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF7C3AED), Color(0xFF6366F1))
)

val RavenCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A2132), Color(0xFF131824))
)

val RavenHeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2E1065).copy(alpha = 0.5f), Color(0xFF082F49).copy(alpha = 0.3f), Color(0xFF0F131D))
)
