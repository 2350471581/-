package com.example.billtracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.billtracker.R

val IncomeGreen = Color(0xFF4CAF7A)
val ExpenseRed = Color(0xFFEA6B5C)
val WechatGreen = Color(0xFF07C160)
val AlipayBlue = Color(0xFF1677FF)
val CardWarmBg = Color(0xFFFFFBF5)

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

@Composable
fun BillTrackerTheme(
    themeIndex: Int = 0,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = Themes.getOrElse(themeIndex) { Themes[0] }
    val colorScheme = if (isDarkTheme) darkColorSchemeFrom(palette) else colorSchemeFrom(palette)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
