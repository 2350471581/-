package com.example.billtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NavTab(
    val label: String,
    val icon: ImageVector
)

@Composable
fun BillTrackerBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab("记账", Icons.Default.Receipt),
        NavTab("计划", Icons.Default.Star),
        NavTab("我的", Icons.Default.Person)
    )

    val primary = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()

    // iOS 风格玻璃背景：半透明基色 + 主题色淡染
    val baseAlpha = 0.45f
    val baseColor = if (isDark) Color(0xFF1A1A1A).copy(alpha = baseAlpha)
                   else Color.White.copy(alpha = baseAlpha)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 背景层
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = baseColor,
            shadowElevation = 6.dp
        ) {
            // 主题色淡染渐变（从顶部到底部渐弱）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.10f),
                                primary.copy(alpha = 0.02f)
                            )
                        )
                    )
            )
        }

        // 内容层
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                BillBottomNavItem(
                    icon = tab.icon,
                    label = tab.label,
                    isSelected = isSelected,
                    onClick = { onTabSelected(index) },
                    primaryColor = primary
                )
            }
        }
    }
}

@Composable
private fun BillBottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val iconTint = if (isSelected) Color.White else primaryColor.copy(alpha = 0.45f)
    val bgColor = if (isSelected) primaryColor else Color.Transparent
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "iconScale")

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        }
    }
}
