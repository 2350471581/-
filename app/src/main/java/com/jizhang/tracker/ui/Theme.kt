package com.jizhang.tracker.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.jizhang.tracker.R

val IncomeGreen = Color(0xFF4CAF7A)
val ExpenseRed = Color(0xFFEA6B5C)
val WechatGreen = Color(0xFF07C160)
val AlipayBlue = Color(0xFF1677FF)

// ── 语义化颜色常量 ──
val SubtleText = Color(0xFF9AA0A6)        // 次要文字/图标
val DividerColor = Color(0xFFF1F3F4)      // 分隔线背景
val MutedIconColor = Color(0xFFBDBDBD)    // 静默图标
val DarkSubtleText = Color(0xFF5F6368)    // 深色次要文字
val FrostedWhite = Color.White.copy(alpha = 0.88f)   // 毛玻璃白
val FrostedDark = Color(0xFF2A2A2A).copy(alpha = 0.88f) // 毛玻璃暗

// ── 通用颜色常量 ──
val DarkTextColor = Color(0xFF1F1F1F)         // 深色模式主文字
val BorderColor = Color(0xFFDADCE0)           // 边框颜色
val WarningOrange = Color(0xFFE65100)         // 警告文字/橙色标签
val AmberWarning = Color(0xFFFF9800)          // 琥珀色警告（计划进度）
val GoogleRed = Color(0xFFEA4335)             // Google 风格错误红色
val OrangeTint = Color(0xFFE8824A)            // 橙色强调
val DisabledGray = Color(0xFFC8C8C8)          // 禁用状态灰色

private val SiYuanHeiTi = FontFamily(
    Font(R.font.siyuanheiti_regular, FontWeight.Normal),
    Font(R.font.siyuanheiti_regular, FontWeight.Medium),
    Font(R.font.siyuanheiti_regular, FontWeight.Bold),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = SiYuanHeiTi, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

private fun computeLuminance(color: Color): Float {
    fun linearize(channel: Float): Float {
        return if (channel <= 0.03928f) channel / 12.92f
        else Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }
    return 0.2126f * linearize(color.red) +
            0.7152f * linearize(color.green) +
            0.0722f * linearize(color.blue)
}

// 如果提取的主色调偏亮 (>0.5)，图片大概率是深色背景 → 使用深色主题（白字）
private fun isImageDark(color: Color): Boolean {
    val lum = computeLuminance(color)
    // 主色调亮度高 → 说明是从暗色图片中提取的高亮色 → 图片偏暗
    return lum > 0.45f
}

private fun ensureContrast(color: Color, isDark: Boolean): Color {
    val (h, s, l) = rgbToHsl(color.red, color.green, color.blue)

    return if (isDark) {
        if (l < 0.5f) hslToRgb(h, s, 0.5f) else color
    } else {
        if (l > 0.45f) hslToRgb(h, s * 0.85f, 0.38f) else color
    }
}

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f)) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Color {
    if (s == 0f) return Color(l, l, l)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun hue2rgb(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    return Color(hue2rgb(h + 1f / 3f), hue2rgb(h), hue2rgb(h - 1f / 3f))
}

@Composable
fun BillTrackerTheme(
    themeIndex: Int = 0,
    isDarkTheme: Boolean = false,
    customConfig: CustomThemeConfig = CustomThemeConfig(),
    content: @Composable () -> Unit
) {
    val isCustomTheme = themeIndex == CUSTOM_THEME_INDEX && customConfig.imageUri.isNotBlank()
    val isMaterialYou = themeIndex == MATERIAL_YOU_THEME_INDEX

    val customDark = isCustomTheme && isImageDark(Color(customConfig.extractedPrimary))
    val effectiveDark = isDarkTheme || customDark

    val colorScheme = if (isMaterialYou) {
        val context = LocalContext.current
        if (Build.VERSION.SDK_INT >= 31) {
            if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val palettes = if (effectiveDark) DarkThemes else Themes
            val p = palettes.getOrElse(themeIndex) { palettes[0] }
            lightColorScheme(
                primary = p.primary,
                background = p.background,
                surface = p.surface,
                surfaceVariant = p.surfaceVariant,
            )
        }
    } else {
        val palette = if (isCustomTheme) {
            val primary = ensureContrast(Color(customConfig.extractedPrimary), effectiveDark)
            ThemePalette(
                name = "自定义",
                primary = primary,
                background = if (effectiveDark) Color(0xFF1A1A1C) else Color(0xFFF5F3FA),
                surface = if (effectiveDark) Color(0xFF242426) else Color(0xFFFAF8FC),
                surfaceVariant = if (effectiveDark) Color(0xFF2D2D30) else Color(0xFFD0CDEE),
                income = IncomeGreen,
                expense = ExpenseRed,
                gradientStart = primary.copy(alpha = 0.2f),
                gradientEnd = primary.copy(alpha = 0.05f)
            )
        } else if (isDarkTheme) {
            DarkThemes.getOrElse(themeIndex) { DarkThemes[0] }
        } else {
            Themes.getOrElse(themeIndex) { Themes[0] }
        }
        if (effectiveDark) darkColorSchemeFrom(palette) else colorSchemeFrom(palette)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
