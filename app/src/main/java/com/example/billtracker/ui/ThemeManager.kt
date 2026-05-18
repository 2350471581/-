package com.example.billtracker.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

const val BLUE_PINK_THEME_INDEX = 4

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
        primary = Color(0xFF7AB86A),
        background = Color(0xFFECF7D8),
        surface = Color(0xFFF2F8E4),
        surfaceVariant = Color(0xFFD8EABC),
        income = Color(0xFF5B9A5A),
        expense = Color(0xFFD4806A),
        gradientStart = Color(0xFFECF7D8),
        gradientEnd = Color(0xFFD8EABC)
    ),
    ThemePalette(
        name = "日落橙",
        primary = Color(0xFFE8C870),
        background = Color(0xFFFFF4E0),
        surface = Color(0xFFFFF8EA),
        surfaceVariant = Color(0xFFF0E2B8),
        income = Color(0xFF7AAC6A),
        expense = Color(0xFFD47A5A),
        gradientStart = Color(0xFFFFF4E0),
        gradientEnd = Color(0xFFF0E2B8)
    ),
    ThemePalette(
        name = "鸢尾蓝",
        primary = Color(0xFFCDDBF7),
        background = Color(0xFFF5F3FA),
        surface = Color(0xFFFAF8FC),
        surfaceVariant = Color(0xFFD0CDEE),
        income = Color(0xFF5A9A6A),
        expense = Color(0xFFD47A5A),
        gradientStart = Color(0xFFCDDBF7),
        gradientEnd = Color(0xFFD0CDEE)
    ),
    ThemePalette(
        name = "樱花粉",
        primary = Color(0xFFECACBA),
        background = Color(0xFFF7F0F0),
        surface = Color(0xFFFBF5F5),
        surfaceVariant = Color(0xFFE8D5D8),
        income = Color(0xFF5B9A5A),
        expense = Color(0xFFD4606A),
        gradientStart = Color(0xFFF7F0F0),
        gradientEnd = Color(0xFFE8D5D8)
    ),
    ThemePalette(
        name = "蓝粉",
        primary = Color(0xFFEF96C5),
        background = Color(0xFFCCFBFF),
        surface = Color(0xFFE8FDFF),
        surfaceVariant = Color(0xFFD4F0F8),
        income = Color(0xFF5B9A5A),
        expense = Color(0xFFE07A9A),
        gradientStart = Color(0xFFCCFBFF),
        gradientEnd = Color(0xFFEF96C5)
    ),
    ThemePalette(
        name = "苋菜红",
        primary = Color(0xFFD4606A),
        background = Color(0xFFFFF0EC),
        surface = Color(0xFFFFF5F0),
        surfaceVariant = Color(0xFFECD0C8),
        income = Color(0xFF5B8C5A),
        expense = Color(0xFFC4604A),
        gradientStart = Color(0xFFFFF0EC),
        gradientEnd = Color(0xFFECD0C8)
    ),
    ThemePalette(
        name = "薰衣草紫",
        primary = Color(0xFF9967CC),
        background = Color(0xFFF0ECF8),
        surface = Color(0xFFF5F2FA),
        surfaceVariant = Color(0xFF9A99D7),
        income = Color(0xFF5B9A5A),
        expense = Color(0xFFD4604A),
        gradientStart = Color(0xFFF0ECF8),
        gradientEnd = Color(0xFF9A99D7)
    ),
    ThemePalette(
        name = "紫幽兰",
        primary = Color(0xFF7A84A0),
        background = Color(0xFFF0F0F0),
        surface = Color(0xFFF5F5F5),
        surfaceVariant = Color(0xFFD8D5E0),
        income = Color(0xFF5B8C5A),
        expense = Color(0xFFD4604A),
        gradientStart = Color(0xFFF0F0F0),
        gradientEnd = Color(0xFFD8D5E0)
    ),
)

val DarkThemes = listOf(
    ThemePalette(
        name = "抹茶绿 (深色)",
        primary = Color(0xFF8AB86A),
        background = Color(0xFF1A1E16),
        surface = Color(0xFF242820),
        surfaceVariant = Color(0xFF2D3228),
        income = Color(0xFF6AB85A),
        expense = Color(0xFFD48A7A),
        gradientStart = Color(0xFF2A3A20),
        gradientEnd = Color(0xFF3A4A2A)
    ),
    ThemePalette(
        name = "日落橙 (深色)",
        primary = Color(0xFFE8C870),
        background = Color(0xFF1C1A16),
        surface = Color(0xFF262420),
        surfaceVariant = Color(0xFF302D28),
        income = Color(0xFF6AB85A),
        expense = Color(0xFFD47A5A),
        gradientStart = Color(0xFF3A3020),
        gradientEnd = Color(0xFF4A3A2A)
    ),
    ThemePalette(
        name = "鸢尾蓝 (深色)",
        primary = Color(0xFFCDDBF7),
        background = Color(0xFF1A1820),
        surface = Color(0xFF221F28),
        surfaceVariant = Color(0xFF2D2A35),
        income = Color(0xFF5AB85A),
        expense = Color(0xFFD47A6A),
        gradientStart = Color(0xFF1A1A28),
        gradientEnd = Color(0xFF1F1D28)
    ),
    ThemePalette(
        name = "樱花粉 (深色)",
        primary = Color(0xFFE8A0B0),
        background = Color(0xFF1C181A),
        surface = Color(0xFF262224),
        surfaceVariant = Color(0xFF302A2C),
        income = Color(0xFF6AB85A),
        expense = Color(0xFFD47A84),
        gradientStart = Color(0xFF3A2830),
        gradientEnd = Color(0xFF4A3040)
    ),
    ThemePalette(
        name = "蓝粉 (深色)",
        primary = Color(0xFFEF96C5),
        background = Color(0xFF1A1A2E),
        surface = Color(0xFF242440),
        surfaceVariant = Color(0xFF3A2A5C),
        income = Color(0xFF5AB85A),
        expense = Color(0xFFE07A9A),
        gradientStart = Color(0xFF2A2A50),
        gradientEnd = Color(0xFF4A2A5C)
    ),
    ThemePalette(
        name = "苋菜红 (深色)",
        primary = Color(0xFFD4606A),
        background = Color(0xFF1C1618),
        surface = Color(0xFF262022),
        surfaceVariant = Color(0xFF30282A),
        income = Color(0xFF5AB85A),
        expense = Color(0xFFC46A5A),
        gradientStart = Color(0xFF3A2020),
        gradientEnd = Color(0xFF4A2828)
    ),
    ThemePalette(
        name = "薰衣草紫 (深色)",
        primary = Color(0xFFB896E0),
        background = Color(0xFF1A1A2E),
        surface = Color(0xFF24243A),
        surfaceVariant = Color(0xFF3A3A5C),
        income = Color(0xFF6AB85A),
        expense = Color(0xFFD47A6A),
        gradientStart = Color(0xFF2A2840),
        gradientEnd = Color(0xFF3A3A5C)
    ),
    ThemePalette(
        name = "紫幽兰 (深色)",
        primary = Color(0xFF8A8EA8),
        background = Color(0xFF1A1A1C),
        surface = Color(0xFF242426),
        surfaceVariant = Color(0xFF2D2D30),
        income = Color(0xFF5AB85A),
        expense = Color(0xFFD47A6A),
        gradientStart = Color(0xFF2A2A38),
        gradientEnd = Color(0xFF30304A)
    ),
)

fun colorSchemeFrom(palette: ThemePalette) = lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.primary.copy(alpha = 0.18f),
    onPrimaryContainer = palette.primary,
    secondary = Color(0xFF7A9E7A),
    onSecondary = Color.White,
    secondaryContainer = palette.primary.copy(alpha = 0.18f),
    onSecondaryContainer = palette.primary,
    background = palette.background,
    onBackground = Color(0xFF2C241A),
    surface = palette.surface,
    onSurface = Color(0xFF2C241A),
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = Color(0xFF5C4E3E),
    error = palette.expense,
    onError = Color.White,
    outline = palette.primary.copy(alpha = 0.25f),
    outlineVariant = palette.surfaceVariant.copy(alpha = 0.6f),
)

fun darkColorSchemeFrom(palette: ThemePalette) = darkColorScheme(
    primary = palette.primary,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = palette.primary.copy(alpha = 0.25f),
    onPrimaryContainer = palette.primary,
    secondary = Color(0xFF66BB8A),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = palette.primary.copy(alpha = 0.25f),
    onSecondaryContainer = palette.primary,
    background = palette.background,
    onBackground = Color(0xFFE8E0D8),
    surface = palette.surface,
    onSurface = Color(0xFFE8E0D8),
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = Color(0xFFB0A898),
    error = palette.expense,
    onError = Color(0xFF1A1A1A),
    outline = palette.primary.copy(alpha = 0.3f),
    outlineVariant = palette.surfaceVariant.copy(alpha = 0.4f),
)

val avatarEmojis = listOf(
    "😀", "😎", "🐱", "🌸", "🌟",
    "🎨", "🍀", "🌈", "🦋", "🌻",
    "🐶", "🐼", "🦊", "🐰", "🦁",
    "🍉", "🍕", "🎵", "📷", "🚀",
    "👻", "🤖", "👑", "💎", "🔥",
    "🌺", "🍁", "⭐", "🎈", "💡"
)
