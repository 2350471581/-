package com.jizhang.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.data.CustomCategory
import com.jizhang.tracker.ui.components.ProfileMenuItem
import com.jizhang.tracker.ui.dialogs.AiPromptDialog
import com.jizhang.tracker.ui.dialogs.CategoriesDialog
import com.jizhang.tracker.ui.dialogs.TriggerKeywordsDialog

@Composable
fun AdvancedSettingsScreen(
    customAiPrompt: String = "",
    onCustomAiPromptChange: (String) -> Unit = {},
    triggerKeywords: Set<String> = emptySet(),
    onAddTriggerKeyword: (String) -> Unit = {},
    onRemoveTriggerKeyword: (String) -> Unit = {},
    customCategories: List<CustomCategory> = emptyList(),
    onAddCustomCategory: (CustomCategory) -> Unit = {},
    onRemoveCustomCategory: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface

    var showAiPromptDialog by remember { mutableStateOf(false) }
    var showTriggerKeywordsDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.profile_section_advanced),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 内容区
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg()),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    ProfileMenuItem(
                        icon = Icons.Default.TextFields,
                        label = stringResource(R.string.profile_custom_ai_prompt),
                        onClick = { showAiPromptDialog = true },
                        tint = primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        label = stringResource(R.string.profile_trigger_keywords),
                        onClick = { showTriggerKeywordsDialog = true },
                        tint = primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.Label,
                        label = stringResource(R.string.profile_manage_categories),
                        onClick = { showCategoriesDialog = true },
                        tint = primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Shield,
                        label = "后台保护",
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        tint = primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    ProfileMenuItem(
                        icon = Icons.Default.Accessibility,
                        label = stringResource(R.string.profile_accessibility),
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        tint = primary
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showAiPromptDialog) {
        AiPromptDialog(
            currentPrompt = customAiPrompt,
            onSave = { onCustomAiPromptChange(it); showAiPromptDialog = false },
            onDismiss = { showAiPromptDialog = false }
        )
    }

    if (showTriggerKeywordsDialog) {
        TriggerKeywordsDialog(
            triggerKeywords = triggerKeywords,
            onAdd = { onAddTriggerKeyword(it); showTriggerKeywordsDialog = false },
            onRemove = onRemoveTriggerKeyword,
            onDismiss = { showTriggerKeywordsDialog = false }
        )
    }

    if (showCategoriesDialog) {
        CategoriesDialog(
            customCategories = customCategories,
            onAdd = { onAddCustomCategory(it); showCategoriesDialog = false },
            onRemove = onRemoveCustomCategory,
            onDismiss = { showCategoriesDialog = false }
        )
    }
}
