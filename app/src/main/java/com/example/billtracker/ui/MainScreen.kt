package com.example.billtracker.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import com.example.billtracker.data.BackupManager
import com.example.billtracker.data.CategoryManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.ui.CUSTOM_THEME_INDEX
import com.example.billtracker.ui.components.BackgroundGradient
import com.example.billtracker.ui.components.CustomThemeBackground
import com.example.billtracker.ui.components.AddTransactionDialog
import com.example.billtracker.ui.components.TransactionDetailCard
import com.example.billtracker.ui.components.FirstLaunchDialog
import com.example.billtracker.ui.components.DateRangeFilterDialog
import com.example.billtracker.ui.ImportScreen
import com.example.billtracker.ui.components.BillTrackerBottomBar
import com.example.billtracker.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val todayTransactions by viewModel.todayTransactions.collectAsStateWithLifecycle()
    val todayIncome by viewModel.todayIncome.collectAsStateWithLifecycle()
    val todayExpense by viewModel.todayExpense.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val filterStartDate by viewModel.filterStartDate.collectAsStateWithLifecycle()
    val filterEndDate by viewModel.filterEndDate.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var showDateFilterDialog by remember { mutableStateOf(false) }
    var detailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var showAIChat by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showAIChatTutorial by remember { mutableStateOf(false) }
    var showAiChatProfileTutorial by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    var lastBackTime by remember { mutableLongStateOf(0L) }

    val isManualMode by viewModel.isManualMode.collectAsStateWithLifecycle()
    val aiChatEnabled by viewModel.aiChatEnabled.collectAsStateWithLifecycle()
    val aiChatTutorialDone by viewModel.aiChatTutorialDone.collectAsStateWithLifecycle()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    var showManualPlanAlert by remember { mutableStateOf(false) }

    val themeIdx by viewModel.themeIndex.collectAsStateWithLifecycle()
    val customThemeCfg by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val gradientPair = remember(themeIdx, isDark) {
        val palette = if (isDark) DarkThemes.getOrElse(themeIdx) { DarkThemes[0] }
        else Themes.getOrElse(themeIdx) { Themes[0] }
        palette.gradientStart to palette.gradientEnd
    }

    // 从通知设置返回时重新检查权限
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.setManualMode(false) // 切换到自动模式
            viewModel.refreshFromSms()
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val count = viewModel.importBackup(it)
                snackbarHostState.showSnackbar("已导入 $count 条账单" + if (count > 0) "" else "（无新数据或数据已存在）")
            }
        }
    }

    // 模式切换回调
    val onToggleMode: () -> Unit = {
        if (isManualMode) {
            // 切换到自动模式 → 请求权限
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            // 切换到手动模式
            viewModel.setManualMode(true)
        }
    }

    // ── 双击返回键退出 ──
    val isInSubScreen = showAIChat || showAIChatTutorial || showAiChatProfileTutorial || showSearch
    val isDialogOpen = showAddDialog || showAddPlanDialog || showDateFilterDialog || detailTransaction != null || showManualPlanAlert || showBackupDialog
    BackHandler(enabled = !isInSubScreen && !isDialogOpen) {
        val now = System.currentTimeMillis()
        if (now - lastBackTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackTime = now
            Toast.makeText(context, "再滑一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        topBar = {},
        floatingActionButton = {
            if (!showAIChat && !showAIChatTutorial && selectedBottomTab != 2 && !showImport && !showSearch) {
            when (selectedBottomTab) {
                0 -> {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (aiChatEnabled) {
                                    if (!aiChatTutorialDone) showAIChatTutorial = true
                                    else showAIChat = true
                                } else showAddDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加账单", modifier = Modifier.size(28.dp))
                        }
                    }
                }
                1 -> {
                    SmallFloatingActionButton(
                        onClick = { showAddPlanDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加计划", modifier = Modifier.size(20.dp))
                    }
                }
            }
            }
        },
        bottomBar = {
            if (!showAIChat && !showImport && !showSearch) {
                BillTrackerBottomBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { selectedBottomTab = it }
                )
            }
        }
    ) { padding ->
        val useCustomBg = themeIdx == CUSTOM_THEME_INDEX && customThemeCfg.imageUri.isNotBlank()
        val scaffoldContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            if (showAIChat) {
                AIChatScreen(
                    onBack = { showAIChat = false },
                    onAddTransaction = { amount, type, desc ->
                        viewModel.addTransaction(amount, type, desc) { success ->
                            if (!success) scope.launch { snackbarHostState.showSnackbar("检测到重复账单，已跳过") }
                        }
                    },
                    viewModel = viewModel,
                    aiService = viewModel.aiBillService
                )
            } else if (showImport) {
                ImportScreen(
                    onBack = { showImport = false },
                    onImport = { bills -> viewModel.importBills(bills) { dup -> scope.launch { snackbarHostState.showSnackbar("已导入 ${bills.size - dup} 条账单" + if (dup > 0) "，跳过 $dup 条重复" else "") } } }
                )
            } else if (showSearch) {
                SearchScreen(onBack = { showSearch = false }, allTransactions = allTransactions)
            } else if (selectedBottomTab == 0) {
                // ── 权限提示条（仅在自动模式下显示） ──
                if (!hasPermission && !isManualMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFF8E1),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "需要短信权限读取微信/支付宝账单",
                                fontSize = 13.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                                Text("授予权限", fontSize = 13.sp, color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // ── 通知监听权限提示（自动模式下 + 已开启短信权限但未开通知权限） ──
                if (!isManualMode && hasPermission && !hasNotificationPermission) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE8F0FE),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "开启通知监听可自动读取微信/支付宝支付通知",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.openNotificationSettings() }) {
                                Text("去开启", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // ── 微信风格余额卡片 ──
                val isAllPage = pagerState.currentPage == 1
                val installDate = viewModel.installDateMillis
                val totalIncome by remember {
                    derivedStateOf {
                        allTransactions.filter { it.type == TransactionType.INCOME && it.dateMillis >= installDate }
                            .sumOf { it.amount }
                    }
                }
                val totalExpense by remember {
                    derivedStateOf {
                        allTransactions.filter { it.type == TransactionType.EXPENSE && it.dateMillis >= installDate }
                            .sumOf { it.amount }
                    }
                }
                WeChatStyleBalanceCard(
                    income = if (isAllPage) totalIncome else todayIncome,
                    expense = if (isAllPage) totalExpense else todayExpense,
                    title = if (isAllPage) "总净收入" else "今日净收入",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    isManualMode = isManualMode,
                    onToggleMode = onToggleMode,
                    onDateFilterClick = { showDateFilterDialog = true },
                    onRefresh = { viewModel.refreshFromSms() },
                    onSearchClick = { showSearch = true }
                )

                // ── 自定义圆角 Tab ──
                val selectedColor = MaterialTheme.colorScheme.primary
                val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        shape = RoundedCornerShape(
                            topStart = 12.dp, bottomStart = 12.dp,
                            topEnd = 0.dp, bottomEnd = 0.dp
                        ),
                        color = if (pagerState.currentPage == 0) selectedColor else CardBg(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "今日明细",
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (pagerState.currentPage == 0) Color.White else unselectedColor,
                            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        shape = RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 12.dp, bottomEnd = 12.dp
                        ),
                        color = if (pagerState.currentPage == 1) selectedColor else CardBg(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "全部记录",
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (pagerState.currentPage == 1) Color.White else unselectedColor,
                            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }

                // ── 页面内容 ──
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val deleteCallback: (Long) -> Unit = { id ->
                        viewModel.deleteTransaction(id)
                        scope.launch { snackbarHostState.showSnackbar("已删除") }
                    }
                    val detailCallback: (TransactionEntity) -> Unit = { tx ->
                        detailTransaction = tx
                    }

                    when (page) {
                        0 -> TransactionList(
                            transactions = todayTransactions,
                            emptyText = "未收取到数据",
                            onDelete = deleteCallback,
                            onItemClick = detailCallback
                        )
                        1 -> TransactionList(
                                transactions = recentTransactions,
                                emptyText = "暂无记录",
                                onDelete = deleteCallback,
                                onItemClick = detailCallback,
                                modifier = Modifier.fillMaxSize()
                            )
                    }
                }

                // ── 隐私声明底部 ──
                PrivacyFooter()
            } else if (selectedBottomTab == 1) {
                // ── 计划页面 ──
                val installDate = viewModel.installDateMillis
                val totalIncomeAll by remember {
                    derivedStateOf {
                        allTransactions.filter { it.type == TransactionType.INCOME && it.dateMillis >= installDate }
                            .sumOf { it.amount }
                    }
                }
                val totalExpenseAll by remember {
                    derivedStateOf {
                        allTransactions.filter { it.type == TransactionType.EXPENSE && it.dateMillis >= installDate }
                            .sumOf { it.amount }
                    }
                }
                PlanScreen(
                    todayIncome = todayIncome,
                    todayExpense = todayExpense,
                    totalIncome = totalIncomeAll,
                    totalExpense = totalExpenseAll,
                    planBalance = viewModel.planBalance.collectAsStateWithLifecycle().value,
                    todayPlanTarget = viewModel.todayPlanTarget.collectAsStateWithLifecycle().value,
                    totalPlanTarget = viewModel.totalPlanTarget.collectAsStateWithLifecycle().value,
                    savePlanTarget = viewModel.savePlanTarget.collectAsStateWithLifecycle().value,
                    todayPlanNote = viewModel.todayPlanNote.collectAsStateWithLifecycle().value,
                    totalPlanNote = viewModel.totalPlanNote.collectAsStateWithLifecycle().value,
                    savePlanNote = viewModel.savePlanNote.collectAsStateWithLifecycle().value,
                    customPlans = viewModel.customPlans.collectAsStateWithLifecycle().value,
                    onBalanceChange = { viewModel.updatePlanBalance(it) },
                    onTodayPlanTargetSave = { viewModel.updateTodayPlanTarget(it) },
                    onTotalPlanTargetSave = { viewModel.updateTotalPlanTarget(it) },
                    onSavePlanTargetSave = { viewModel.updateSavePlanTarget(it) },
                    onTodayPlanNoteSave = { viewModel.updateTodayPlanNote(it) },
                    onTotalPlanNoteSave = { viewModel.updateTotalPlanNote(it) },
                    onSavePlanNoteSave = { viewModel.updateSavePlanNote(it) },
                    onUpdateCustomPlan = { index, target, note -> viewModel.updateCustomPlan(index, target, note) },
                    onDeleteCustomPlan = { index -> viewModel.deleteCustomPlan(index) }
                )
            } else if (selectedBottomTab == 2) {
                // ── 账单分析 ──
                AnalysisScreen(viewModel = viewModel)
            } else if (selectedBottomTab == 3) {
                // ── 我的页面 ──
                val nick by viewModel.nickname.collectAsStateWithLifecycle()
                val avatar by viewModel.avatarEmoji.collectAsStateWithLifecycle()
                val customUri by viewModel.customAvatarUri.collectAsStateWithLifecycle()
                ProfileScreen(
                    nickname = nick,
                    avatarEmoji = avatar,
                    customAvatarUri = customUri,
                    themeIndex = themeIdx,
                    onNicknameChange = { viewModel.setNickname(it) },
                    onAvatarChange = { viewModel.setAvatarEmoji(it) },
                    onCustomAvatarChange = { viewModel.setCustomAvatarUri(it) },
                    onThemeChange = {
                        viewModel.setThemeIndex(it)
                        scope.launch { snackbarHostState.showSnackbar("主题已切换") }
                    },
                    onClearAllData = {
                        viewModel.clearAllData()
                        scope.launch { snackbarHostState.showSnackbar("已清空全部记录") }
                    },


                    onNavigateToImport = { showImport = true },
                    aiChatEnabled = aiChatEnabled,
                    onAiChatToggle = { enabled ->
                        viewModel.setAiChatEnabled(enabled)
                        if (enabled) showAiChatProfileTutorial = true
                    },
                    followSystemTheme = viewModel.followSystemTheme.collectAsStateWithLifecycle().value,
                    onFollowSystemThemeChange = { viewModel.setFollowSystemTheme(it) },
                    customThemeConfig = customThemeCfg,
                    onCustomThemeChange = { viewModel.setCustomThemeConfig(it) },
                    onExportCsv = { start, end ->
                        scope.launch {
                            val uri = viewModel.exportCsv(
                                if (start > 0) start else 0L,
                                if (end > 0) end else System.currentTimeMillis()
                            )
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "导出表格"))
                            } else {
                                snackbarHostState.showSnackbar("导出失败")
                            }
                        }
                    },
                    onExportImage = { start, end ->
                        scope.launch {
                            val uri = viewModel.exportImage(
                                if (start > 0) start else 0L,
                                if (end > 0) end else System.currentTimeMillis()
                            )
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "导出图片"))
                            } else {
                                snackbarHostState.showSnackbar("导出图片失败")
                            }
                        }
                    },
                    onBackupRestore = { showBackupDialog = true }
                )
            }
        }
        }  // scaffoldContent

        if (useCustomBg) {
            CustomThemeBackground(config = customThemeCfg) { scaffoldContent() }
        } else {
            BackgroundGradient(
                gradientStart = gradientPair.first,
                gradientEnd = gradientPair.second
            ) { scaffoldContent() }
        }
    }

    // ── 添加计划弹窗 ──
    if (showAddPlanDialog) {
        AddPlanDialog(
            onDismiss = { showAddPlanDialog = false },
            onConfirm = { name, amount, note, type ->
                viewModel.addCustomPlan(name, amount, note, type)
                showAddPlanDialog = false
            }
        )
    }

    // ── 添加账单弹窗 ──
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, type, note ->
                viewModel.addTransaction(amount, type, note) { success ->
                    if (!success) {
                        scope.launch { snackbarHostState.showSnackbar("检测到重复账单，已跳过") }
                    }
                }
                showAddDialog = false
            }
        )
    }

    // ── 备份恢复弹窗 ──
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("备份恢复", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showBackupDialog = false
                            scope.launch {
                                val uri = viewModel.exportBackup()
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "导出备份"))
                                } else {
                                    snackbarHostState.showSnackbar("导出备份失败")
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导出备份")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            showBackupDialog = false
                            backupImportLauncher.launch("application/json")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导入恢复")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "导出为 JSON 备份文件，包含全部账单、计划和设置",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ── 交易详情卡（50%屏，居中，圆角） ──
    detailTransaction?.let { tx ->
        TransactionDetailCard(
            transaction = tx,
            onNoteSave = { id, note -> viewModel.updateTransactionNote(id, note) },
            onDismiss = { detailTransaction = null }
        )
    }

    // ── 手动模式计划页面提示 ──
    LaunchedEffect(selectedBottomTab, isManualMode) {
        if (selectedBottomTab == 1 && isManualMode) {
            showManualPlanAlert = true
        }
    }
    if (showManualPlanAlert) {
        AlertDialog(
            onDismissRequest = { showManualPlanAlert = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("提示", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Text("请打开自动模式", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = { showManualPlanAlert = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("知道了")
                }
            }
        )
    }

    // ── AI 记账使用教程弹窗 ──
    if (showAIChatTutorial) {
        var countdown by remember { mutableIntStateOf(3) }
        val enabled = countdown <= 0
        LaunchedEffect(Unit) { while (countdown > 0) { delay(1000); countdown-- } }
        AlertDialog(
            onDismissRequest = { if (enabled) { showAIChatTutorial = false; viewModel.markAiChatTutorialDone(); showAIChat = true } },
            shape = RoundedCornerShape(20.dp),
            title = { Text("AI 聊天记账", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column {
                    listOf(
                        "💬 像聊天一样记账",
                        "输入「中午吃饭花了35块」即可快速记账",
                        "",
                        "🤖 AI 智能识别",
                        "自动提取金额、类别，支持 DeepSeek 大模型",
                        "",
                        "📋 确认流程",
                        "AI 识别后点击「确认添加」才真正记账",
                        "",
                        "📱 本地兜底",
                        "网络不可用时自动使用本地规则识别"
                    ).forEach { line ->
                        Text(
                            text = line,
                            fontSize = if (line.startsWith("💬") || line.startsWith("🤖") || line.startsWith("📋") || line.startsWith("📱")) 15.sp else 13.sp,
                            fontWeight = if (line.startsWith("💬") || line.startsWith("🤖") || line.startsWith("📋") || line.startsWith("📱")) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (line.startsWith("💬") || line.startsWith("🤖") || line.startsWith("📋") || line.startsWith("📱")) MaterialTheme.colorScheme.onBackground else DarkSubtleText,
                            modifier = Modifier.padding(top = if (line.isBlank()) 4.dp else 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAIChatTutorial = false; viewModel.markAiChatTutorialDone(); showAIChat = true },
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text(if (enabled) "知道了" else "($countdown) 知道了", color = Color.White) }
            }
        )
    }

    // ── AI 助手开启提示弹窗 ──
    if (showAiChatProfileTutorial) {
        var countdown by remember { mutableIntStateOf(3) }
        val enabled = countdown <= 0
        LaunchedEffect(Unit) { while (countdown > 0) { delay(1000); countdown-- } }
        AlertDialog(
            onDismissRequest = { if (enabled) showAiChatProfileTutorial = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("AI 助手已开启", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤖", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("AI 聊天记账已开启", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击记账助手的 + 号按钮即可使用 AI 自然语言记账",
                        fontSize = 14.sp, color = DarkSubtleText, lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiChatProfileTutorial = false },
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (enabled) "知道了" else "($countdown) 知道了", color = Color.White)
                }
            }
        )
    }

    // ── 首次启动弹窗 ──
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
    if (isFirstLaunch) {
        FirstLaunchDialog(
            onDismiss = { viewModel.dismissFirstLaunch() }
        )
    }

    // ── 日期范围过滤弹窗 ──
    if (showDateFilterDialog) {
        DateRangeFilterDialog(
            initialStart = filterStartDate,
            initialEnd = filterEndDate,
            onConfirm = { start, end ->
                viewModel.filterStartDate.value = start
                viewModel.filterEndDate.value = end
                showDateFilterDialog = false
            },
            onReset = {
                viewModel.filterStartDate.value = null
                viewModel.filterEndDate.value = null
                showDateFilterDialog = false
            },
            onDismiss = { showDateFilterDialog = false }
        )
    }
}

// ── 微信风格余额卡片 ──
@Composable
fun WeChatStyleBalanceCard(
    income: Double,
    expense: Double,
    title: String = "今日净收入",
    modifier: Modifier = Modifier,
    isManualMode: Boolean = true,
    onToggleMode: (() -> Unit)? = null,
    onDateFilterClick: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null
) {
    val net = income - expense

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 小标题 + 日期筛选 + 手动/自动切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (onSearchClick != null) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = SubtleText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (onDateFilterClick != null) {
                    IconButton(
                        onClick = onDateFilterClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "选择日期范围",
                            tint = SubtleText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (!isManualMode && onRefresh != null) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (onToggleMode != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DividerColor
                    ) {
                        Row(
                            modifier = Modifier.height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isManualMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable(enabled = !isManualMode) { onToggleMode() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "手动",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isManualMode) Color.White else SubtleText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (!isManualMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable(enabled = isManualMode) { onToggleMode() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "自动",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isManualMode) Color.White else SubtleText
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // 大金额（微信零钱风格）
            Text(
                text = if (net >= 0) "+¥%.2f".format(net) else "-¥%.2f".format(-net),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0) IncomeGreen else ExpenseRed,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 收入/支出双栏（各占50%，整体放大）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "↑",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("收入", fontSize = 16.sp, color = SubtleText)
                        Text(
                            "¥%.2f".format(income),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(DividerColor)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "↓",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("支出", fontSize = 16.sp, color = SubtleText)
                        Text(
                            "¥%.2f".format(expense),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

// ── 底部隐私文字 ──
@Composable
fun PrivacyFooter() {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = subtle,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "你的隐私数据仅保存在本地，不会上传",
                fontSize = 11.sp,
                color = subtle
            )
        }
    }
}

// ── 交易列表 ──
@Composable
fun TransactionList(
    transactions: List<TransactionEntity>,
    emptyText: String,
    onDelete: (Long) -> Unit,
    onItemClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val prevCount = remember { mutableIntStateOf(transactions.size) }

    // 新增条目时自动滚动到顶部
    LaunchedEffect(transactions.size) {
        if (transactions.size > prevCount.intValue && transactions.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
        prevCount.intValue = transactions.size
    }

    if (transactions.isEmpty()) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        val hint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        Box(
            modifier = Modifier.fillMaxSize().then(modifier),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = emptyText,
                    color = muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击右下角 + 手动记账",
                    color = hint,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().then(modifier),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDelete = { onDelete(transaction.id) },
                    onItemClick = { onItemClick(transaction) }
                )
            }
        }
    }
}

// ── 带左滑操作的交易条目 ──
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val buttonAreaPx = with(LocalDensity.current) { 180.dp.toPx() }

    Box(modifier = Modifier.clipToBounds().fillMaxWidth()) {

        // ── 背景层：左滑后露出的操作按钮 ──
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 详情按钮（打开详情卡片）
            Surface(
                onClick = {
                    scope.launch { offsetX.animateTo(0f) }
                    onItemClick()
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("详情", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // 删除按钮
            Surface(
                onClick = {
                    scope.launch { offsetX.animateTo(0f) }
                    onDelete()
                },
                shape = RoundedCornerShape(10.dp),
                color = ExpenseRed
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }

        // ── 前景层：卡片（可点击进入详情、可左滑） ──
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clickable { onItemClick() }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -buttonAreaPx / 2) {
                                    offsetX.animateTo(-buttonAreaPx, tween(200))
                                } else {
                                    offsetX.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(-buttonAreaPx, 0f)
                                )
                            }
                        }
                    )
                }
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧色条
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(64.dp)
                        .background(
                            if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                            shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
                        )
                )

                Spacer(modifier = Modifier.width(14.dp))

                // 来源标签
                val sourceColor = when (transaction.source) {
                    TransactionSource.WECHAT -> WechatGreen
                    TransactionSource.ALIPAY -> AlipayBlue
                    TransactionSource.MANUAL -> SubtleText
                    TransactionSource.BANK -> Color(0xFFE65100)
                }
                val sourceIcon = when (transaction.source) {
                    TransactionSource.WECHAT -> "微信"
                    TransactionSource.ALIPAY -> "支付宝"
                    TransactionSource.MANUAL -> "其他"
                    TransactionSource.BANK -> "银行"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = sourceColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = sourceIcon,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = sourceColor
                    )
                }

                // ── 分类标签 ──
                if (transaction.category != "其他") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${CategoryManager.getCategoryIcon(transaction.category)} ${transaction.category}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 描述 + 时间
                Column(modifier = Modifier.weight(1f)) {
                    val displayText = transaction.description
                        .replace(Regex("""【[^】]*】"""), "")
                        .trim()
                        .take(25)
                    Text(
                        text = displayText.ifEmpty {
                            if (transaction.source == TransactionSource.MANUAL) "手动记账"
                            else if (transaction.source == TransactionSource.BANK) "银行账单"
                            else "账单"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val timeStr = remember(transaction.dateMillis) {
                            val sdf = SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault())
                            sdf.format(Date(transaction.dateMillis))
                        }
                        Text(text = timeStr, fontSize = 11.sp, color = SubtleText)
                        if (transaction.description.contains("\n--备注--\n")) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "注",
                                fontSize = 9.sp,
                                color = MutedIconColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 金额
                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> IncomeGreen
                    TransactionType.EXPENSE -> ExpenseRed
                }
                val typePrefix = when (transaction.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                }
                Text(
                    text = "$typePrefix¥%.2f".format(transaction.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 14.dp),
                    color = amountColor
                )
            }
        }
    }
}



