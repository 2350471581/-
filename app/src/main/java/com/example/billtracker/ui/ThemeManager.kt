package com.example.billtracker.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class ThemePalette(
    val name: String,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val income: Color,
    val expense: Color
)

val Themes = listOf(
    ThemePalette(
        name = "暖橙",
        primary = Color(0xFFE8824A),
        background = Color(0xFFFFF9F2),
        surface = Color(0xFFFFFDF9),
        surfaceVariant = Color(0xFFFFF0E0),
        income = Color(0xFF4CAF7A),
        expense = Color(0xFFEA6B5C)
    ),
    ThemePalette(
        name = "抹茶绿",
        primary = Color(0xFF6B9E6B),
        background = Color(0xFFF6FAF4),
        surface = Color(0xFFFBFDF9),
        surfaceVariant = Color(0xFFEAF0E8),
        income = Color(0xFF4CAF7A),
        expense = Color(0xFFE8846B)
    ),
    ThemePalette(
        name = "静谧蓝",
        primary = Color(0xFF5B8DB8),
        background = Color(0xFFF4F8FB),
        surface = Color(0xFFF9FCFD),
        surfaceVariant = Color(0xFFE8EFF5),
        income = Color(0xFF4CAF7A),
        expense = Color(0xFFE86B6B)
    ),
    ThemePalette(
        name = "樱花粉",
        primary = Color(0xFFD4889A),
        background = Color(0xFFFFF8F9),
        surface = Color(0xFFFFFCFC),
        surfaceVariant = Color(0xFFFFF0F2),
        income = Color(0xFF7DB87A),
        expense = Color(0xFFE86B7A)
    ),
    ThemePalette(
        name = "经典蓝",
        primary = Color(0xFF1A73E8),
        background = Color(0xFFF8F9FA),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1F3F4),
        income = Color(0xFF34A853),
        expense = Color(0xFFEA4335)
    ),
)

val DarkThemes = listOf(
    ThemePalette(
        name = "暖橙 (深色)",
        primary = Color(0xFFE8824A),
        background = Color(0xFF1A1A1A),
        surface = Color(0xFF252525),
        surfaceVariant = Color(0xFF2D2D2D),
        income = Color(0xFF66BB8A),
        expense = Color(0xFFEF7A6C)
    ),
    ThemePalette(
        name = "抹茶绿 (深色)",
        primary = Color(0xFF6B9E6B),
        background = Color(0xFF1A1A1A),
        surface = Color(0xFF252525),
        surfaceVariant = Color(0xFF2D2D2D),
        income = Color(0xFF66BB8A),
        expense = Color(0xFFEF7A6C)
    ),
    ThemePalette(
        name = "静谧蓝 (深色)",
        primary = Color(0xFF7AAED4),
        background = Color(0xFF1A1A1A),
        surface = Color(0xFF252525),
        surfaceVariant = Color(0xFF2D2D2D),
        income = Color(0xFF66BB8A),
        expense = Color(0xFFEF7A6C)
    ),
    ThemePalette(
        name = "樱花粉 (深色)",
        primary = Color(0xFFD4889A),
        background = Color(0xFF1A1A1A),
        surface = Color(0xFF252525),
        surfaceVariant = Color(0xFF2D2D2D),
        income = Color(0xFF66BB8A),
        expense = Color(0xFFEF7A6C)
    ),
    ThemePalette(
        name = "经典蓝 (深色)",
        primary = Color(0xFF4A90D9),
        background = Color(0xFF1A1A1A),
        surface = Color(0xFF252525),
        surfaceVariant = Color(0xFF2D2D2D),
        income = Color(0xFF66BB8A),
        expense = Color(0xFFEF7A6C)
    ),
)

fun colorSchemeFrom(palette: ThemePalette) = lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.primary.copy(alpha = 0.15f),
    onPrimaryContainer = palette.primary.copy(alpha = 0.8f),
    secondary = Color(0xFF4A9E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F5E4),
    background = palette.background,
    onBackground = Color(0xFF2C241A),
    surface = palette.surface,
    onSurface = Color(0xFF2C241A),
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = Color(0xFF5C4E3E),
    error = Color(0xFFD8604A),
    onError = Color.White,
    outline = Color(0xFFE8DDD0),
)

fun darkColorSchemeFrom(palette: ThemePalette) = darkColorScheme(
    primary = palette.primary,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = palette.primary.copy(alpha = 0.2f),
    onPrimaryContainer = palette.primary.copy(alpha = 0.9f),
    secondary = Color(0xFF66BB8A),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF1A3A2A),
    background = palette.background,
    onBackground = Color(0xFFE8E8E8),
    surface = palette.surface,
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFEF7A6C),
    onError = Color(0xFF1A1A1A),
    outline = Color(0xFF4A4A4A),
)

val avatarEmojis = listOf("😀", "😎", "🐱", "🌸", "🌟", "🎨", "🍀", "🌈", "🦋", "🌻")
