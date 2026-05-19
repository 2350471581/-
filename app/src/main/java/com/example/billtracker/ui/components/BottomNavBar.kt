package com.example.billtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
        NavTab("分析", Icons.Default.Analytics),
        NavTab("我的", Icons.Default.Person)
    )

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp)
                .padding(bottom = 4.dp),
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
    val iconTint = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.45f)
    val bgColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "iconScale")

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .semantics {
                stateDescription = if (isSelected) "已选中" else "未选中"
            },
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
