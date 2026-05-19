package com.jizhang.tracker.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.ui.components.ProfileMenuItem
import com.jizhang.tracker.ui.dialogs.AvatarDialog
import com.jizhang.tracker.ui.dialogs.CustomThemeConfigDialog
import com.jizhang.tracker.ui.dialogs.ExportDialog
import com.jizhang.tracker.ui.dialogs.NicknameDialog
import com.jizhang.tracker.ui.dialogs.ThemeDialog
import com.jizhang.tracker.ui.dialogs.UpdateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    nickname: String,
    avatarEmoji: Int,
    customAvatarUri: String,
    themeIndex: Int,
    onNicknameChange: (String) -> Unit,
    onAvatarChange: (Int) -> Unit,
    onCustomAvatarChange: (String) -> Unit,
    onThemeChange: (Int) -> Unit,
    onClearAllData: () -> Unit,

    onNavigateToImport: () -> Unit = {},
    onExportCsv: (startMillis: Long, endMillis: Long) -> Unit,
    onExportImage: (startMillis: Long, endMillis: Long) -> Unit = { _, _ -> },
    onBackupRestore: () -> Unit = {},
    aiChatEnabled: Boolean = false,
    onAiChatToggle: (Boolean) -> Unit = {},
    customThemeConfig: CustomThemeConfig = CustomThemeConfig(),
    onCustomThemeChange: (CustomThemeConfig) -> Unit = {},
    triggerKeywords: Set<String> = emptySet(),
    onAddTriggerKeyword: (String) -> Unit = {},
    onRemoveTriggerKeyword: (String) -> Unit = {},
    customCategories: List<com.jizhang.tracker.data.CustomCategory> = emptyList(),
    onAddCustomCategory: (com.jizhang.tracker.data.CustomCategory) -> Unit = {},
    onRemoveCustomCategory: (String) -> Unit = {},
    customAiPrompt: String = "",
    onCustomAiPromptChange: (String) -> Unit = {},
) {
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAdvancedScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onCustomAvatarChange(it.toString()) }
    }

    val customBitmap = remember(customAvatarUri) {
        if (customAvatarUri.isNotBlank()) {
            try {
                val uri = Uri.parse(customAvatarUri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (_: Exception) { null }
        } else null
    }

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 顶部主题渐变背景 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primary,
                            primary.copy(alpha = 0.6f),
                            primary.copy(alpha = 0f)
                        )
                    )
                )
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── 头部：头像 + 昵称 ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        onClick = { showAvatarDialog = true },
                        shape = CircleShape,
                        color = if (customBitmap != null) Color.Transparent else primaryContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.profile_avatar_cd),
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = avatarEmojis.getOrElse(avatarEmoji) { "😀" },
                                    fontSize = 40.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.clickable { showNicknameDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nickname,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.profile_edit_nickname_cd),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── 功能入口卡片 ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg())
                ) {
                    Text(
                        text = stringResource(R.string.profile_section_features),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                    )
                    ProfileMenuItem(Icons.Default.Share, stringResource(R.string.profile_export), { showExportDialog = true }, tint = primary)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.FileUpload, stringResource(R.string.profile_import), { onNavigateToImport() }, tint = primary)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.CloudUpload, stringResource(R.string.profile_backup), { onBackupRestore() }, tint = primary)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── 设置卡片 ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg())
                ) {
                    Text(
                        text = stringResource(R.string.profile_section_settings),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                    )
                    ProfileMenuItem(Icons.Default.Tune, stringResource(R.string.profile_section_advanced), { showAdvancedScreen = true }, tint = primary)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.Palette, stringResource(R.string.profile_theme), { showThemeDialog = true }, tint = primary)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.Science, "AI 助手", { onAiChatToggle(!aiChatEnabled) },
                        tint = primary,
                        trailing = {
                            if (aiChatEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.SystemUpdate, stringResource(R.string.profile_check_update), { showUpdateDialog = true }, tint = primary)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.Info, stringResource(R.string.profile_about), { showAboutScreen = true }, tint = primary)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ProfileMenuItem(Icons.Default.DeleteForever, stringResource(R.string.profile_clear_all), { showClearConfirm = true },
                        tint = ExpenseRed)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        // ── 关于全屏页面 ──
        if (showAboutScreen) {
            AboutScreen(onBack = { showAboutScreen = false })
        }

        if (showAdvancedScreen) {
            AdvancedSettingsScreen(
                customAiPrompt = customAiPrompt,
                onCustomAiPromptChange = onCustomAiPromptChange,
                triggerKeywords = triggerKeywords,
                onAddTriggerKeyword = onAddTriggerKeyword,
                onRemoveTriggerKeyword = onRemoveTriggerKeyword,
                customCategories = customCategories,
                onAddCustomCategory = onAddCustomCategory,
                onRemoveCustomCategory = onRemoveCustomCategory,
                onBack = { showAdvancedScreen = false }
            )
        }
    }

    // ── 清空确认弹窗 ──
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.profile_clear_all_title), fontWeight = FontWeight.Bold, color = ExpenseRed) },
            text = { Text(stringResource(R.string.profile_clear_confirm_text)) },
            confirmButton = {
                Button(
                    onClick = { onClearAllData(); showClearConfirm = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) { Text(stringResource(R.string.profile_confirm_clear), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.profile_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // ── 弹窗 ──
    if (showNicknameDialog) {
        NicknameDialog(
            currentName = nickname,
            onSave = onNicknameChange,
            onDismiss = { showNicknameDialog = false }
        )
    }

    if (showAvatarDialog) {
        AvatarDialog(
            currentIndex = avatarEmoji,
            customAvatarUri = customAvatarUri,
            onEmojiSelect = onAvatarChange,
            onImagePickerLaunch = { imagePickerLauncher.launch("image/*") },
            onClearCustom = { onCustomAvatarChange("") },
            onDismiss = { showAvatarDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentThemeIndex = themeIndex,
            hasCustomImage = customThemeConfig.imageUri.isNotBlank(),
            onThemeChange = onThemeChange,
            onCustomThemeClick = { showCustomThemeDialog = true },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showCustomThemeDialog) {
        CustomThemeConfigDialog(
            config = customThemeConfig,
            onConfirm = { newConfig ->
                onCustomThemeChange(newConfig)
                showCustomThemeDialog = false
            },
            onDismiss = { showCustomThemeDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onExportCsv = onExportCsv,
            onExportImage = onExportImage,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismiss = { showUpdateDialog = false }
        )
    }

}
