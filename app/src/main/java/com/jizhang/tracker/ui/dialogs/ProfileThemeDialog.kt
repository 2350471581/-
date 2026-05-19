package com.jizhang.tracker.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.CUSTOM_THEME_INDEX
import com.jizhang.tracker.ui.MATERIAL_YOU_THEME_INDEX
import com.jizhang.tracker.ui.Themes

@Composable
fun ThemeDialog(
    currentThemeIndex: Int,
    hasCustomImage: Boolean,
    onThemeChange: (Int) -> Unit,
    onCustomThemeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(stringResource(R.string.profile_theme_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // ── 自定义 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onThemeChange(CUSTOM_THEME_INDEX)
                            onDismiss()
                            onCustomThemeClick()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasCustomImage) stringResource(R.string.profile_custom_checked) else stringResource(R.string.profile_custom),
                        fontSize = 16.sp,
                        color = if (currentThemeIndex == CUSTOM_THEME_INDEX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (currentThemeIndex == CUSTOM_THEME_INDEX) FontWeight.Bold else FontWeight.Normal
                    )
                    if (currentThemeIndex == CUSTOM_THEME_INDEX) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                // ── 跟随系统壁纸配色 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onThemeChange(MATERIAL_YOU_THEME_INDEX)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFF7B8CC4), modifier = Modifier.size(28.dp)) {}
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.profile_material_you), fontSize = 16.sp,
                        color = if (currentThemeIndex == MATERIAL_YOU_THEME_INDEX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (currentThemeIndex == MATERIAL_YOU_THEME_INDEX) FontWeight.Bold else FontWeight.Normal)
                    if (currentThemeIndex == MATERIAL_YOU_THEME_INDEX) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(4.dp))

                // ── 各主题 ──
                Themes.forEachIndexed { i, theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onThemeChange(i)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = theme.primary,
                            modifier = Modifier.size(28.dp)
                        ) {}
                        Spacer(Modifier.width(14.dp))
                        Text(theme.name, fontSize = 16.sp,
                            color = if (i == currentThemeIndex) theme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (i == currentThemeIndex) FontWeight.Bold else FontWeight.Normal)
                        if (i == currentThemeIndex) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = theme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_close), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
