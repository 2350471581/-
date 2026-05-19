package com.example.billtracker.ui

import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billtracker.ui.components.ProfileMenuItem
import com.example.billtracker.ui.components.TagChip
import com.example.billtracker.ui.dialogs.CustomThemeConfigDialog
import com.example.billtracker.ui.dialogs.ManualDownloadDialog
import com.example.billtracker.ui.dialogs.ManualUpdateDialog

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
    followSystemTheme: Boolean = false,
    onFollowSystemThemeChange: (Boolean) -> Unit = {},
    customThemeConfig: CustomThemeConfig = CustomThemeConfig(),
    onCustomThemeChange: (CustomThemeConfig) -> Unit = {},
) {
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateState by remember { mutableStateOf("") } // checking | available | up_to_date | downloading | downloaded | error
    var updateInfo by remember { mutableStateOf<com.example.billtracker.data.UpdateInfo?>(null) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateError by remember { mutableStateOf("") }
    var downloadedFile by remember { mutableStateOf<java.io.File?>(null) }
    var showManualDownloadDialog by remember { mutableStateOf(false) }
    var showManualUpdateDialog by remember { mutableStateOf(false) }
    val updateScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 导出时间范围
    var exportTimeRange by remember { mutableIntStateOf(0) } // 0=全部 1=30天 2=自定义
    var exportStartDate by remember { mutableStateOf(0L) }
    var exportEndDate by remember { mutableStateOf(0L) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onCustomAvatarChange(it.toString()) }
    }

    // 加载自定义头像 bitmap
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

    val isDark = isSystemInDarkTheme()
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
        // ── 头部：头像 + 昵称 ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像（带主题色描边）
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
                                contentDescription = "头像",
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
                // 昵称
                Row(
                    modifier = Modifier.clickable { showNicknameDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nickname,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑昵称",
                        tint = if (isDark) Color.White.copy(alpha = 0.6f) else primary.copy(alpha = 0.6f),
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
                    text = "功能",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                )
                ProfileMenuItem(Icons.Default.Share, "导出账单", { showExportDialog = true }, tint = primary)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.FileUpload, "导入账单", { onNavigateToImport() }, tint = primary)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.CloudUpload, "备份恢复", { onBackupRestore() }, tint = primary)
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
                    text = "设置",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                )
                ProfileMenuItem(Icons.Default.Palette, "更换主题", { showThemeDialog = true }, tint = primary)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.Science, "AI 助手", { onAiChatToggle(!aiChatEnabled) },
                    tint = primary,
                    trailing = {
                        if (aiChatEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6DD98E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.SystemUpdate, "检查更新", { showUpdateDialog = true }, tint = primary)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.Info, "关于", { showAboutScreen = true }, tint = primary)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.DeleteForever, "清空全部记录", { showClearConfirm = true },
                    tint = Color(0xFFEA6B5C))
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }

    // ── 关于全屏页面 ──
    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
    }
}

// ── 编辑昵称弹窗 ──
    if (showNicknameDialog) {
        var input by remember { mutableStateOf(nickname) }
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("修改昵称", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(16) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { onNicknameChange(input); showNicknameDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // ── 选择头像弹窗 ──
    if (showAvatarDialog) {
        Dialog(
            onDismissRequest = { showAvatarDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg())
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("选择头像", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))

                    // ── 从相册选择 ──
                    Surface(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showAvatarDialog = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("从相册选择", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("选择表情", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(avatarEmojis.size) { i ->
                            Surface(
                                onClick = { onAvatarChange(i); showAvatarDialog = false },
                                shape = CircleShape,
                                color = if (i == avatarEmoji) MaterialTheme.colorScheme.primaryContainer
                                       else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(avatarEmojis[i], fontSize = 24.sp)
                                }
                            }
                        }
                    }

                    // ── 清除自定义头像 ──
                    if (customAvatarUri.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { onCustomAvatarChange("") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("清除自定义头像", color = Color(0xFFEA6B5C))
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAvatarDialog = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // ── 主题选择弹窗 ──
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("更换主题", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // ── 跟随系统 ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFollowSystemThemeChange(!followSystemTheme) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("跟随系统", fontSize = 16.sp,
                            color = if (followSystemTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (followSystemTheme) FontWeight.Bold else FontWeight.Normal)
                        if (followSystemTheme) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // ── 自定义 ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (followSystemTheme) onFollowSystemThemeChange(false)
                                onThemeChange(CUSTOM_THEME_INDEX)
                                showThemeDialog = false
                                showCustomThemeDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (customThemeConfig.imageUri.isNotBlank()) "自定义 ✓" else "自定义",
                            fontSize = 16.sp,
                            color = if (themeIndex == CUSTOM_THEME_INDEX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (themeIndex == CUSTOM_THEME_INDEX) FontWeight.Bold else FontWeight.Normal
                        )
                        if (themeIndex == CUSTOM_THEME_INDEX) {
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
                                    if (followSystemTheme) onFollowSystemThemeChange(false)
                                    onThemeChange(i)
                                    showThemeDialog = false
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
                                color = if (!followSystemTheme && i == themeIndex) theme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (!followSystemTheme && i == themeIndex) FontWeight.Bold else FontWeight.Normal)
                            if (!followSystemTheme && i == themeIndex) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = theme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // ── 自定义主题配置弹窗 ──
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

    // ── 清空确认弹窗 ──
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("清空全部记录", fontWeight = FontWeight.Bold, color = Color(0xFFEA6B5C)) },
            text = { Text("确定要删除所有账单数据吗？此操作不可恢复。") },
            confirmButton = {
                Button(
                    onClick = { onClearAllData(); showClearConfirm = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA6B5C))
                ) { Text("确定清空", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    // ── 导出选项弹窗 ──
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("导出账单", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 时间范围选择
                    Text("时间范围", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("全部时间", "最近30天", "自定义").forEachIndexed { i, label ->
                            TagChip(
                                selected = exportTimeRange == i,
                                onClick = {
                                    exportTimeRange = i
                                    if (i == 0) { exportStartDate = 0L; exportEndDate = 0L }
                                    else if (i == 1) {
                                        exportEndDate = 0L
                                        exportStartDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                                    }
                                },
                                label = label
                            )
                        }
                    }

                    // 自定义日期选择
                    if (exportTimeRange == 2) {
                        Spacer(Modifier.height(10.dp))
                        Text("开始日期", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                if (exportStartDate > 0) cal.timeInMillis = exportStartDate
                                android.app.DatePickerDialog(
                                    context, { _, year, month, day ->
                                        cal.set(year, month, day, 0, 0, 0)
                                        cal.set(java.util.Calendar.MILLISECOND, 0)
                                        exportStartDate = cal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val fmt = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                            Text(
                                text = if (exportStartDate > 0) fmt.format(java.util.Date(exportStartDate)) else "点击选择",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = if (exportStartDate > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("结束日期", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                if (exportEndDate > 0) cal.timeInMillis = exportEndDate
                                android.app.DatePickerDialog(
                                    context, { _, year, month, day ->
                                        cal.set(year, month, day, 23, 59, 59)
                                        cal.set(java.util.Calendar.MILLISECOND, 999)
                                        exportEndDate = cal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val fmt = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                            Text(
                                text = if (exportEndDate > 0) fmt.format(java.util.Date(exportEndDate)) else "点击选择",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = if (exportEndDate > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = {
                            showExportDialog = false
                            val s = if (exportTimeRange == 0) 0L else exportStartDate
                            val e = if (exportTimeRange == 0) 0L else exportEndDate
                            onExportCsv(s, e)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("导出表格 (CSV)", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("以表格形式导出账单数据", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = {
                            showExportDialog = false
                            val s = if (exportTimeRange == 0) 0L else exportStartDate
                            val e = if (exportTimeRange == 0) 0L else exportEndDate
                            onExportImage(s, e)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("导出图片", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("以图片形式导出账单概览", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }


    // ── 检查更新弹窗 ──
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (updateState != "downloading") showUpdateDialog = false
            },
            shape = RoundedCornerShape(20.dp),
            title = { Text("检查更新", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (updateState) {
                        "" -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "当前版本：v${com.example.billtracker.data.AppUpdater.getCurrentVersionName(context)}",
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    updateState = "checking"
                                    updateScope.launch {
                                        val result = com.example.billtracker.data.AppUpdater.checkUpdate(context)
                                        when (result) {
                                            is com.example.billtracker.data.UpdateResult.Available -> {
                                                updateInfo = result.info
                                                updateState = "available"
                                            }
                                            is com.example.billtracker.data.UpdateResult.UpToDate -> {
                                                updateState = "up_to_date"
                                            }
                                            is com.example.billtracker.data.UpdateResult.Error -> {
                                                updateError = result.message
                                                updateState = "error"
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("检查更新") }
                        }
                        "checking" -> {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("正在检查...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        "up_to_date" -> {
                            Spacer(Modifier.height(12.dp))
                            Text("✅", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("已是最新版本", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        "available" -> {
                            val info = updateInfo ?: return@Column
                            Spacer(Modifier.height(4.dp))
                            Text("发现新版本 v${info.versionName}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = info.releaseNotes,
                                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // 源测速结果
                            Column(modifier = Modifier.fillMaxWidth()) {
                                info.sources.forEach { src ->
                                    val status = if (src.latencyMs >= 0) "${src.latencyMs}ms" else "不可用"
                                    val statusColor = when {
                                        src.latencyMs < 0 -> Color(0xFFEA6B5C)
                                        src.latencyMs < 500 -> IncomeGreen
                                        src.latencyMs < 2000 -> Color(0xFFE8824A)
                                        else -> ExpenseRed
                                    }
                                    Row(
                                        modifier = Modifier.padding(vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = src.label,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        updateState = "downloading"
                                        updateScope.launch {
                                            val result = com.example.billtracker.data.AppUpdater.downloadApk(context, info) { pct ->
                                                downloadProgress = pct
                                            }
                                            when (result) {
                                                is com.example.billtracker.data.DownloadResult.Success -> {
                                                    downloadedFile = result.file
                                                    updateState = "downloaded"
                                                }
                                                is com.example.billtracker.data.DownloadResult.Error -> {
                                                    updateError = result.message
                                                    updateState = "error"
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("下载更新") }
                                Button(
                                    onClick = { showManualUpdateDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("手动更新") }
                            }
                            if (info.lanzouUrl.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showManualDownloadDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("手动下载") }
                            }
                        }
                        "downloading" -> {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("下载中 $downloadProgress%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        "downloaded" -> {
                            Spacer(Modifier.height(12.dp))
                            Text("✅", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("下载完成", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    downloadedFile?.let { file ->
                                        com.example.billtracker.data.AppUpdater.installApk(context, file)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("安装") }
                        }
                        "error" -> {
                            Spacer(Modifier.height(12.dp))
                            Text("❌", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("更新失败", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(updateError, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { updateState = "" },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("重试") }
                                Button(
                                    onClick = { showManualUpdateDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("手动更新") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (updateState != "downloading") {
                        showUpdateDialog = false
                        updateState = ""
                        downloadProgress = 0
                        updateInfo = null
                        downloadedFile = null
                    }
                }) {
                    Text(if (updateState == "downloading") "下载中..." else "关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ── 手动下载弹窗（蓝奏云） ──
    if (showManualDownloadDialog) {
        val lanzouUrl = updateInfo?.lanzouUrl ?: ""
        val lanzouPassword = updateInfo?.lanzouPassword ?: ""
        ManualDownloadDialog(
            url = lanzouUrl,
            password = lanzouPassword,
            onDismiss = { showManualDownloadDialog = false }
        )
    }

    // ── 手动更新弹窗（显示所有源 + 蓝奏云） ──
    if (showManualUpdateDialog) {
        val info = updateInfo
        ManualUpdateDialog(
            versionName = info?.versionName ?: "",
            onDismiss = { showManualUpdateDialog = false }
        )
    }
}
