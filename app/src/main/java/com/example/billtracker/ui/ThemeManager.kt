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
    val expense: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

val Themes = listOf(
    ThemePalette(
        name = "抹茶绿",
        primary = Color(0xFF7AA85A),
        background = Color(0xFFF5F8F2),
        surface = Color(0xFFFAFCF9),
        surfaceVariant = Color(0xFFEAF0E4),
        income = Color(0xFF5B9A5A),
        expense = Color(0xFFD4806A),
        gradientStart = Color(0xFFF5FBF2),
        gradientEnd = Color(0xFFA0C888)
    ),
    ThemePalette(
        name = "暖橙",
        primary = Color(0xFFD4784A),
        background = Color(0xFFFDF6F0),
        surface = Color(0xFFFFFCF9),
        surfaceVariant = Color(0xFFF5EDE4),
        income = Color(0xFF5B8C5A),
        expense = Color(0xFFD4604A),
        gradientStart = Color(0xFFFDF6F0),
        gradientEnd = Color(0xFFE8C8A8)
    ),
    ThemePalette(
        name = "静谧蓝",
        primary = Color(0xFF6A8FA8),
        background = Color(0xFFF4F7F9),
        surface = Color(0xFFF9FBFD),
        surfaceVariant = Color(0xFFE8EEF2),
        income = Color(0xFF5B8C5A),
        expense = Color(0xFFD4605A),
        gradientStart = Color(0xFFF2F8FC),
        gradientEnd = Color(0xFFB8D4E8)
    ),
    ThemePalette(
        name = "樱花粉",
        primary = Color(0xFFC48A9A),
        background = Color(0xFFFDF8F9),
        surface = Color(0xFFFFFCFC),
        surfaceVariant = Color(0xFFF5EEF0),
        income = Color(0xFF6A9E6A),
        expense = Color(0xFFD4606A),
        gradientStart = Color(0xFFFDF8F9),
        gradientEnd = Color(0xFFE8C8D0)
    ),
    ThemePalette(
        name = "经典蓝",
        primary = Color(0xFF3A7BD5),
        background = Color(0xFFF8F9FB),
        surface = Color.White,
        surfaceVariant = Color(0xFFF0F2F4),
        income = Color(0xFF34A853),
        expense = Color(0xFFD4604A),
        gradientStart = Color(0xFFF2F6FC),
        gradientEnd = Color(0xFFA8C8E8)
    ),
)

val DarkThemes = listOf(
    ThemePalette(
        name = "抹茶绿 (深色)",
        primary = Color(0xFF7AB87A),
        background = Color(0xFF1A1F1A),
        surface = Color(0xFF242924),
        surfaceVariant = Color(0xFF2D322D),
        income = Color(0xFF66DD8A),
        expense = Color(0xFFEF7A6C),
        gradientStart = Color(0xFF1A1F1A),
        gradientEnd = Color(0xFF2A3F2A)
    ),
    ThemePalette(
        name = "暖橙 (深色)",
        primary = Color(0xFFD48060),
        background = Color(0xFF1F1A18),
        surface = Color(0xFF292422),
        surfaceVariant = Color(0xFF322D2B),
        income = Color(0xFF66DD8A),
        expense = Color(0xFFEF6A5C),
        gradientStart = Color(0xFF1F1A18),
        gradientEnd = Color(0xFF3F2A20)
    ),
    ThemePalette(
        name = "静谧蓝 (深色)",
        primary = Color(0xFF6AAED4),
        background = Color(0xFF181A1F),
        surface = Color(0xFF222429),
        surfaceVariant = Color(0xFF2B2D32),
        income = Color(0xFF66DD8A),
        expense = Color(0xFFEF6A5C),
        gradientStart = Color(0xFF181A1F),
        gradientEnd = Color(0xFF20303F)
    ),
    ThemePalette(
        name = "樱花粉 (深色)",
        primary = Color(0xFFD09AAA),
        background = Color(0xFF1F181A),
        surface = Color(0xFF292224),
        surfaceVariant = Color(0xFF322B2D),
        income = Color(0xFF66DD8A),
        expense = Color(0xFFEF6A5C),
        gradientStart = Color(0xFF1F181A),
        gradientEnd = Color(0xFF3F2028)
    ),
    ThemePalette(
        name = "经典蓝 (深色)",
        primary = Color(0xFF4A90E0),
        background = Color(0xFF18181F),
        surface = Color(0xFF222229),
        surfaceVariant = Color(0xFF2B2B32),
        income = Color(0xFF66DD8A),
        expense = Color(0xFFEF6A5C),
        gradientStart = Color(0xFF18181F),
        gradientEnd = Color(0xFF20283F)
    ),
)

fun colorSchemeFrom(palette: ThemePalette) = lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.primary.copy(alpha = 0.12f),
    onPrimaryContainer = palette.primary,
    secondary = Color(0xFF7A9E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0EDE0),
    background = palette.background,
    onBackground = Color(0xFF2C241A),
    surface = palette.surface,
    onSurface = Color(0xFF2C241A),
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = Color(0xFF5C4E3E),
    error = palette.expense,
    onError = Color.White,
    outline = Color(0xFFE0D8CC),
)

fun darkColorSchemeFrom(palette: ThemePalette) = darkColorScheme(
    primary = palette.primary,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = palette.primary.copy(alpha = 0.2f),
    onPrimaryContainer = palette.primary,
    secondary = Color(0xFF66BB8A),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF1A3A2A),
    background = palette.background,
    onBackground = Color(0xFFE8E0D8),
    surface = palette.surface,
    onSurface = Color(0xFFE8E0D8),
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = Color(0xFFB0A898),
    error = palette.expense,
    onError = Color(0xFF1A1A1A),
    outline = Color(0xFF4A4844),
)

val avatarEmojis = listOf(
    "😀", "😎", "🐱", "🌸", "🌟",
    "🎨", "🍀", "🌈", "🦋", "🌻",
    "🐶", "🐼", "🦊", "🐰", "🦁",
    "🍉", "🍕", "🎵", "📷", "🚀",
    "👻", "🤖", "👑", "💎", "🔥",
    "🌺", "🍁", "⭐", "🎈", "💡"
)
