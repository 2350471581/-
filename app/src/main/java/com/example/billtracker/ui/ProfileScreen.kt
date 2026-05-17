package com.example.billtracker.ui

import kotlinx.coroutines.launch
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billtracker.R

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
    onNavigateToAnalysis: () -> Unit,
    onNavigateToImport: () -> Unit = {},
    onExportCsv: (startMillis: Long, endMillis: Long) -> Unit,
    onExportImage: (startMillis: Long, endMillis: Long) -> Unit = { _, _ -> },
    aiChatEnabled: Boolean = false,
    onAiChatToggle: (Boolean) -> Unit = {},
    followSystemTheme: Boolean = false,
    onFollowSystemThemeChange: (Boolean) -> Unit = {}
) {
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
    val frostedCardColor = if (isDark) Color(0xFF2A2A2A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // ── 头部：头像 + 昵称 ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 头像
                Surface(
                    onClick = { showAvatarDialog = true },
                    shape = CircleShape,
                    color = if (customBitmap != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
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
                                fontSize = 36.sp
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑昵称",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = frostedCardColor)
            ) {
                ProfileMenuItem(Icons.Default.Analytics, "账单分析", { onNavigateToAnalysis() })
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.Share, "导出账单", { showExportDialog = true })
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.FileUpload, "导入账单", { onNavigateToImport() })
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── 设置卡片 ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = frostedCardColor)
            ) {
                Text(
                    text = "设置",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                )
                ProfileMenuItem(Icons.Default.Palette, "更换主题", { showThemeDialog = true })
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.Science, "AI 助手", { onAiChatToggle(!aiChatEnabled) },
                    trailing = {
                        if (aiChatEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF07C160)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    })
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.SystemUpdate, "检查更新", { showUpdateDialog = true })
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ProfileMenuItem(Icons.Default.Info, "关于", { showAboutScreen = true })
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                colors = CardDefaults.cardColors(containerColor = frostedCardColor)
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌓", fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text("跟随系统", fontSize = 16.sp,
                            color = if (followSystemTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (followSystemTheme) FontWeight.Bold else FontWeight.Normal)
                        if (followSystemTheme) {
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
                                .clickable { onThemeChange(i); showThemeDialog = false }
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
                            FilterChip(
                                selected = exportTimeRange == i,
                                onClick = {
                                    exportTimeRange = i
                                    if (i == 0) { exportStartDate = 0L; exportEndDate = 0L }
                                    else if (i == 1) {
                                        exportEndDate = 0L
                                        exportStartDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                                    }
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
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
                            Spacer(Modifier.height(16.dp))
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
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("下载更新") }
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
                            Text("检查失败", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(updateError, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { updateState = "" },
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("重试") }
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
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp, color = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

// ── 关于页面 ──
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF8F9FA)
    var showIntroDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

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
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "关于",
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

                // ── 1. 作者邮箱（直接显示） ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2A2A2A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "▎作者邮箱",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "3175878672@qq.com",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── 2. 应用介绍（点击弹窗） ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showIntroDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2A2A2A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "▎应用介绍",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "记账助手 v${com.example.billtracker.data.AppUpdater.getCurrentVersionName(context)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── 3. 赞赏作者（点击弹窗） ──
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDonateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2A2A2A).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "▎赞赏作者",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "如果这个应用对你有帮助，欢迎赞赏支持！",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── 介绍弹窗 ──
    if (showIntroDialog) {
        AlertDialog(
            onDismissRequest = { showIntroDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("应用介绍", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "记账助手",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "v${com.example.billtracker.data.AppUpdater.getCurrentVersionName(context)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "记账助手是一款轻量级的个人记账应用，帮你轻松管理日常收支。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "▎主要功能",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    val features = listOf(
                        "手动/自动记账：支持手动输入和自动读取通知栏账单",
                        "AI 聊天记账：自然语言描述即可记账",
                        "智能分类：自动识别交易类别（餐饮、交通、购物等）",
                        "账单分析：收支对比柱状图、支出分类饼图",
                        "计划管理：设定预算目标，跟踪消费进度",
                        "多主题：支持多种温馨配色主题",
                        "数据导出：支持 CSV 表格和图片导出，可选择时间范围"
                    )
                    features.forEach { f ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text("•  ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = f,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "▎数据安全",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "所有账单数据仅保存在您的设备本地，不会上传到任何服务器。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntroDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ── 赞赏弹窗 ──
    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("赞赏作者", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "如果这个应用对你有帮助，欢迎赞赏支持！",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.zsm),
                        contentDescription = "赞赏码",
                        modifier = Modifier.size(240.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "微信 / 支付宝 扫码赞赏",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
