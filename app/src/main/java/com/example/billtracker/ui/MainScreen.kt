package com.example.billtracker.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
    var showAnalysis by remember { mutableStateOf(false) }
    var showAIChat by remember { mutableStateOf(false) }
    var showAIChatTutorial by remember { mutableStateOf(false) }
    var showAiChatProfileTutorial by remember { mutableStateOf(false) }

    var lastBackTime by remember { mutableLongStateOf(0L) }

    val isManualMode by viewModel.isManualMode.collectAsStateWithLifecycle()
    val aiChatEnabled by viewModel.aiChatEnabled.collectAsStateWithLifecycle()
    val aiChatTutorialDone by viewModel.aiChatTutorialDone.collectAsStateWithLifecycle()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    var showManualPlanAlert by remember { mutableStateOf(false) }

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
    val isInSubScreen = showAIChat || showAnalysis || showAIChatTutorial || showAiChatProfileTutorial
    val isDialogOpen = showAddDialog || showAddPlanDialog || showDateFilterDialog || detailTransaction != null || showManualPlanAlert
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
            if (!showAIChat && !showAIChatTutorial && !showAnalysis) {
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
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        selected = selectedBottomTab == 0,
                        icon = Icons.Default.Receipt,
                        label = "记账助手",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBottomTab = 0 }
                    )
                    BottomNavItem(
                        selected = selectedBottomTab == 1,
                        icon = Icons.Default.Star,
                        label = "计划",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBottomTab = 1 }
                    )
                    BottomNavItem(
                        selected = selectedBottomTab == 2,
                        icon = Icons.Default.Person,
                        label = "我的",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedBottomTab = 2 }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            if (showAnalysis) {
                AnalysisScreen(viewModel = viewModel, onBack = { showAnalysis = false })
            } else if (showAIChat) {
                AIChatScreen(
                    onBack = { showAIChat = false },
                    onAddTransaction = { amount, type, desc ->
                        viewModel.addTransaction(amount, type, desc)
                    },
                    viewModel = viewModel
                )
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
                    onRefresh = { viewModel.refreshFromSms() }
                )

                // ── TabRow ──
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("今日明细", fontWeight = if (pagerState.currentPage == 0) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("全部记录", fontWeight = if (pagerState.currentPage == 1) FontWeight.SemiBold else FontWeight.Normal) }
                    )
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
                // ── 我的页面 ──
                val nick by viewModel.nickname.collectAsStateWithLifecycle()
                val avatar by viewModel.avatarEmoji.collectAsStateWithLifecycle()
                val themeIdx by viewModel.themeIndex.collectAsStateWithLifecycle()
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
                    onNavigateToAnalysis = { showAnalysis = true },
                    aiChatEnabled = aiChatEnabled,
                    onAiChatToggle = { enabled ->
                        viewModel.setAiChatEnabled(enabled)
                        if (enabled) showAiChatProfileTutorial = true
                    },
                    followSystemTheme = viewModel.followSystemTheme.collectAsStateWithLifecycle().value,
                    onFollowSystemThemeChange = { viewModel.setFollowSystemTheme(it) },
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
                    }
                )
            }
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
                viewModel.addTransaction(amount, type, note)
                showAddDialog = false
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
                            color = if (line.startsWith("💬") || line.startsWith("🤖") || line.startsWith("📋") || line.startsWith("📱")) MaterialTheme.colorScheme.onBackground else Color(0xFF5F6368),
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
                        fontSize = 14.sp, color = Color(0xFF5F6368), lineHeight = 20.sp,
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
    onRefresh: (() -> Unit)? = null
) {
    val net = income - expense

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                if (onDateFilterClick != null) {
                    IconButton(
                        onClick = onDateFilterClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "选择日期范围",
                            tint = Color(0xFF9AA0A6),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(32.dp)
                    ) {
                        // 手动
                        Surface(
                            onClick = {
                                if (!isManualMode) onToggleMode()
                            },
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                            color = if (isManualMode) MaterialTheme.colorScheme.primary else Color(0xFFF1F3F4)
                        ) {
                            Text(
                                text = "手动",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isManualMode) Color.White else Color(0xFF9AA0A6),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        // 自动
                        Surface(
                            onClick = {
                                if (isManualMode) onToggleMode()
                            },
                            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                            color = if (!isManualMode) MaterialTheme.colorScheme.primary else Color(0xFFF1F3F4)
                        ) {
                            Text(
                                text = "自动",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isManualMode) Color.White else Color(0xFF9AA0A6),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
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
                    .background(Color(0xFFF1F3F4))
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
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("收入", fontSize = 16.sp, color = Color(0xFF9AA0A6))
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
                        .background(Color(0xFFF1F3F4))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("支出", fontSize = 16.sp, color = Color(0xFF9AA0A6))
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8F9FA)
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
                tint = Color(0xFF9AA0A6),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "你的隐私数据仅保存在本地，不会上传",
                fontSize = 11.sp,
                color = Color(0xFF9AA0A6)
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
        Box(
            modifier = Modifier.fillMaxSize().then(modifier),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = Color(0xFFDADCE0),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = emptyText,
                    color = Color(0xFF9AA0A6),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "点击右下角 + 手动记账",
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
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
                        Icons.Default.Message,
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    TransactionSource.MANUAL -> Color(0xFF9AA0A6)
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
                        color = Color(0xFFF1F3F4)
                    ) {
                        Text(
                            text = transaction.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF5F6368)
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
                        color = Color(0xFF1F1F1F)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val timeStr = remember(transaction.dateMillis) {
                            val sdf = SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault())
                            sdf.format(Date(transaction.dateMillis))
                        }
                        Text(text = timeStr, fontSize = 11.sp, color = Color(0xFF9AA0A6))
                        if (transaction.description.contains("\n--备注--\n")) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "注",
                                fontSize = 9.sp,
                                color = Color(0xFFBDBDBD),
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

// ── 手动记账弹窗 ──
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: TransactionType, note: String) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("添加账单", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("支出") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.12f),
                            selectedLabelColor = ExpenseRed
                        )
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("收入") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = 0.12f),
                            selectedLabelColor = IncomeGreen
                        )
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; amountError = false },
                    label = { Text("金额") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) {{ Text("请输入有效金额", color = ExpenseRed) }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    placeholder = { Text("例如：午饭、打车...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                    } else {
                        onConfirm(amount, type, note)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("添加", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF5F6368))
            }
        }
    )
}

// ── 交易详情卡（50%屏高，居中，圆角） ──
@Composable
fun TransactionDetailCard(
    transaction: TransactionEntity,
    onNoteSave: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val marker = "\n--备注--\n"
    val originalSms = remember(transaction) {
        val idx = transaction.description.indexOf(marker)
        if (idx >= 0) transaction.description.substring(0, idx) else transaction.description
    }
    val existingNotes = remember(transaction) {
        val idx = transaction.description.indexOf(marker)
        if (idx >= 0) transaction.description.substring(idx + marker.length) else ""
    }

    var notesInput by remember { mutableStateOf(existingNotes) }

    val sourceLabel = when (transaction.source) {
        TransactionSource.WECHAT -> "微信"
        TransactionSource.ALIPAY -> "支付宝"
        TransactionSource.MANUAL -> "其他"
        TransactionSource.BANK -> "银行"
    }
    val sourceColor = when (transaction.source) {
        TransactionSource.WECHAT -> WechatGreen
        TransactionSource.ALIPAY -> AlipayBlue
        TransactionSource.MANUAL -> Color(0xFF9AA0A6)
        TransactionSource.BANK -> Color(0xFFE65100)
    }
    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
    }
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
    }
    val typePrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
    }

    val dateStr = remember(transaction.dateMillis) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.dateMillis))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.55f),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── 头部：来源标签 + 类型 + 时间 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sourceColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = sourceLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = sourceColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = typeLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5F6368)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = dateStr, fontSize = 11.sp, color = Color(0xFF9AA0A6))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 大金额 ──
                Text(
                    text = "$typePrefix¥%.2f".format(transaction.amount),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 原始短信（只读灰底） ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F3F4)
                ) {
                    Text(
                        text = originalSms,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF5F6368),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 分隔线 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF1F3F4))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 备注输入 ──
                Text(
                    text = "备注",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5F6368)
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = { Text("添加备注...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFDADCE0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── 取消 / 保存按钮 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF5F6368))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newDesc = if (notesInput.isBlank()) {
                                originalSms
                            } else {
                                "$originalSms$marker$notesInput"
                            }
                            onNoteSave(transaction.id, newDesc)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("保存", color = Color.White)
                    }
                }
            }
        }
    }
}

// ── 底部导航项 ──
@Composable
private fun BottomNavItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9AA0A6)
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color
        )
    }
}

// ── 首次启动弹窗（简化版） ──
@Composable
fun FirstLaunchDialog(
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(3) }
    val enabled = countdown <= 0

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Dialog(
        onDismissRequest = { if (enabled) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.55f),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // 标题
                    Text(
                        text = "欢迎使用记账助手",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                    Spacer(Modifier.height(12.dp))

                    // 可滑动内容
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val features = listOf(
                            "📝 手动/自动记账" to "手动输入或自动读取通知栏账单",
                            "🤖 AI 聊天记账" to "自然语言描述即可快速记账",
                            "📊 账单分析" to "收支柱状图与分类饼图",
                            "🎯 计划管理" to "设定预算目标，跟踪进度",
                            "🎨 个性主题" to "多种温馨配色自由切换",
                            "📤 数据导出" to "支持CSV和图片导出"
                        )
                        features.forEach { (title, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold)
                                Column {
                                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF3C4043))
                                    Text(desc, fontSize = 12.sp, color = Color(0xFF9AA0A6))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "所有数据仅保存在本地，不上传任何服务器。",
                            fontSize = 12.sp,
                            color = Color(0xFF9AA0A6),
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    // 按钮
                    Button(
                        onClick = onDismiss,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color(0xFFBDBDBD)
                        )
                    ) {
                        Text(
                            text = if (enabled) "开始使用" else "($countdown) 开始使用",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── 日期范围过滤弹窗 ──
@Composable
private fun DateRangeFilterDialog(
    initialStart: Long?,
    initialEnd: Long?,
    onConfirm: (start: Long?, end: Long?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    fun showDatePicker(current: Long, onSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onSelected(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("选择日期范围", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("开始日期", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5F6368))
                Surface(
                    onClick = { showDatePicker(startDate ?: System.currentTimeMillis()) { startDate = it } },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F3F4)
                ) {
                    Text(
                        text = if (startDate != null) dateFormat.format(Date(startDate!!)) else "点击选择",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        fontSize = 14.sp,
                        color = if (startDate != null) Color(0xFF1F1F1F) else Color(0xFF9AA0A6)
                    )
                }

                Text("结束日期", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5F6368))
                Surface(
                    onClick = { showDatePicker(endDate ?: System.currentTimeMillis()) { endDate = it } },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F3F4)
                ) {
                    Text(
                        text = if (endDate != null) dateFormat.format(Date(endDate!!)) else "点击选择",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        fontSize = 14.sp,
                        color = if (endDate != null) Color(0xFF1F1F1F) else Color(0xFF9AA0A6)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startDate, endDate) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("确定", modifier = Modifier.padding(horizontal = 8.dp))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text("重置", color = Color(0xFF5F6368))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color(0xFF5F6368))
                }
            }
        }
    )
}
