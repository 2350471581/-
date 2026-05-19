package com.jizhang.tracker.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jizhang.tracker.data.PlanDataType
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.ui.components.BackgroundGradient
import com.jizhang.tracker.ui.components.CustomThemeBackground
import com.jizhang.tracker.ui.components.BillTrackerBottomBar
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jizhang.tracker.data.AppUpdater
import com.jizhang.tracker.data.UpdateResult
import com.jizhang.tracker.viewmodel.LedgerViewModel
import com.jizhang.tracker.viewmodel.PlanViewModel
import com.jizhang.tracker.viewmodel.AnalysisViewModel
import com.jizhang.tracker.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    ledgerViewModel: LedgerViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel(),
    analysisViewModel: AnalysisViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val todayTransactions by ledgerViewModel.todayTransactions.collectAsStateWithLifecycle()
    val todayIncome by ledgerViewModel.todayIncome.collectAsStateWithLifecycle()
    val todayExpense by ledgerViewModel.todayExpense.collectAsStateWithLifecycle()
    val allTransactions by ledgerViewModel.allTransactions.collectAsStateWithLifecycle()
    val recentTransactions by ledgerViewModel.recentTransactions.collectAsStateWithLifecycle()
    val filterStartDate by ledgerViewModel.filterStartDate.collectAsStateWithLifecycle()
    val filterEndDate by ledgerViewModel.filterEndDate.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddPlanDialog by remember { mutableStateOf(false) }
    var showDateFilterDialog by remember { mutableStateOf(false) }
    var detailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val tabForRoute: (String?) -> Int = { route ->
        when (route) {
            MainRoute.PLAN -> 1
            MainRoute.ANALYSIS -> 2
            MainRoute.PROFILE -> 3
            else -> 0
        }
    }
    var showAIChatTutorial by remember { mutableStateOf(false) }
    var showAiChatProfileTutorial by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var lastBackTime by remember { mutableLongStateOf(0L) }

    val isManualMode by ledgerViewModel.isManualMode.collectAsStateWithLifecycle()
    val aiChatEnabled by profileViewModel.aiChatEnabled.collectAsStateWithLifecycle()
    val aiChatTutorialDone by profileViewModel.aiChatTutorialDone.collectAsStateWithLifecycle()
    val hasNotificationPermission by ledgerViewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    var showManualPlanAlert by remember { mutableStateOf(false) }
    val isRefreshing by ledgerViewModel.isRefreshing.collectAsStateWithLifecycle()

    val themeIdx by profileViewModel.themeIndex.collectAsStateWithLifecycle()
    val customThemeCfg by profileViewModel.customThemeConfig.collectAsStateWithLifecycle()
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
                ledgerViewModel.checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current

    // 手动模式计划页面提示
    LaunchedEffect(currentRoute, isManualMode) {
        if (currentRoute == MainRoute.PLAN && isManualMode) {
            showManualPlanAlert = true
        }
    }

    // ── 首页更新横幅 ──
    var updateVersionName by remember { mutableStateOf<String?>(null) }
    var updateVersionCode by remember { mutableIntStateOf(0) }
    val updatePrefs = remember { context.getSharedPreferences("app_updater", 0) }
    var dismissedUpdateVersion by remember { mutableIntStateOf(updatePrefs.getInt("dismissed_update_version", 0)) }

    // 首页启动时检查更新
    LaunchedEffect(Unit) {
        val result = AppUpdater.checkUpdate(context)
        if (result is UpdateResult.Available && result.info.versionCode > dismissedUpdateVersion) {
            updateVersionName = result.info.versionName
            updateVersionCode = result.info.versionCode
        }
    }

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
            ledgerViewModel.setManualMode(false) // 切换到自动模式
            ledgerViewModel.refreshFromSms()
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val count = profileViewModel.importBackup(it)
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
            ledgerViewModel.setManualMode(true)
        }
    }

    // ── 双击返回键退出 / 导航回首页 ──
    val isDialogOpen = showAddDialog || showAddPlanDialog || showDateFilterDialog || detailTransaction != null || showManualPlanAlert || showBackupDialog
    val isModalRoute = currentRoute in listOf(MainRoute.AI_CHAT, MainRoute.SEARCH, MainRoute.IMPORT)
    BackHandler(enabled = !isDialogOpen && !showAIChatTutorial && !showAiChatProfileTutorial) {
        if (currentRoute == MainRoute.LEDGER || currentRoute == null) {
            val now = System.currentTimeMillis()
            if (now - lastBackTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackTime = now
                Toast.makeText(context, "再滑一次退出", Toast.LENGTH_SHORT).show()
            }
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        topBar = {},
        floatingActionButton = {
            if (!isModalRoute && !showAIChatTutorial) {
            when (currentRoute) {
                MainRoute.LEDGER -> {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (aiChatEnabled) {
                                    if (!aiChatTutorialDone) showAIChatTutorial = true
                                    else navController.navigate(MainRoute.AI_CHAT)
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
                MainRoute.PLAN -> {
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
            if (!isModalRoute) {
                BillTrackerBottomBar(
                    selectedTab = tabForRoute(currentRoute),
                    onTabSelected = { index ->
                        val route = when (index) {
                            1 -> MainRoute.PLAN
                            2 -> MainRoute.ANALYSIS
                            3 -> MainRoute.PROFILE
                            else -> MainRoute.LEDGER
                        }
                        navController.navigate(route) {
                            popUpTo(MainRoute.LEDGER) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
            NavHost(
                    navController = navController,
                    startDestination = MainRoute.LEDGER,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(MainRoute.LEDGER) {
                        LedgerPage(
                            todayTransactions = todayTransactions,
                            recentTransactions = recentTransactions,
                            todayIncome = todayIncome,
                            todayExpense = todayExpense,
                            allTransactions = allTransactions,
                            isManualMode = isManualMode,
                            hasSmsPermission = hasPermission,
                            hasNotificationPermission = hasNotificationPermission,
                            installDateMillis = ledgerViewModel.installDateMillis,
                            onRequestSmsPermission = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                            onOpenNotificationSettings = { ledgerViewModel.openNotificationSettings() },
                            onToggleMode = onToggleMode,
                            onRefresh = { ledgerViewModel.refreshFromSms() },
                            onSearchClick = { navController.navigate(MainRoute.SEARCH) },
                            onDateFilterClick = { showDateFilterDialog = true },
                            onDeleteTransaction = { id -> ledgerViewModel.deleteTransaction(id) },
                            onDetailClick = { tx -> detailTransaction = tx },
                            snackbarHostState = snackbarHostState,
                            updateVersionName = updateVersionName,
                            onUpdateNow = { showUpdateDialog = true },
                            onDismissUpdate = {
                                updatePrefs.edit().putInt("dismissed_update_version", updateVersionCode).apply()
                                updateVersionName = null
                            },
                            isRefreshing = isRefreshing
                        )
                    }
                    composable(MainRoute.PLAN) {
                        // ── 计划页面 ──
                        val installDate = ledgerViewModel.installDateMillis
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
                            planBalance = planViewModel.planBalance.collectAsStateWithLifecycle().value,
                            todayPlanTarget = planViewModel.todayPlanTarget.collectAsStateWithLifecycle().value,
                            totalPlanTarget = planViewModel.totalPlanTarget.collectAsStateWithLifecycle().value,
                            savePlanTarget = planViewModel.savePlanTarget.collectAsStateWithLifecycle().value,
                            todayPlanNote = planViewModel.todayPlanNote.collectAsStateWithLifecycle().value,
                            totalPlanNote = planViewModel.totalPlanNote.collectAsStateWithLifecycle().value,
                            savePlanNote = planViewModel.savePlanNote.collectAsStateWithLifecycle().value,
                            customPlans = planViewModel.customPlans.collectAsStateWithLifecycle().value,
                            onBalanceChange = { planViewModel.updatePlanBalance(it) },
                            onTodayPlanTargetSave = { planViewModel.updateTodayPlanTarget(it) },
                            onTotalPlanTargetSave = { planViewModel.updateTotalPlanTarget(it) },
                            onSavePlanTargetSave = { planViewModel.updateSavePlanTarget(it) },
                            onTodayPlanNoteSave = { planViewModel.updateTodayPlanNote(it) },
                            onTotalPlanNoteSave = { planViewModel.updateTotalPlanNote(it) },
                            onSavePlanNoteSave = { planViewModel.updateSavePlanNote(it) },
                            onUpdateCustomPlan = { index, target, note -> planViewModel.updateCustomPlan(index, target, note) },
                            onDeleteCustomPlan = { index -> planViewModel.deleteCustomPlan(index) }
                        )
                    }
                    composable(MainRoute.ANALYSIS) {
                        // ── 账单分析 ──
                        AnalysisScreen(viewModel = analysisViewModel)
                    }
                    composable(MainRoute.PROFILE) {
                        // ── 我的页面 ──
                        val nick by profileViewModel.nickname.collectAsStateWithLifecycle()
                        val avatar by profileViewModel.avatarEmoji.collectAsStateWithLifecycle()
                        val customUri by profileViewModel.customAvatarUri.collectAsStateWithLifecycle()
                        val triggerKw by profileViewModel.triggerKeywords.collectAsStateWithLifecycle()
                        val customCats by profileViewModel.customCategories.collectAsStateWithLifecycle()
                        val aiPrompt by profileViewModel.customAiPrompt.collectAsStateWithLifecycle()
                        ProfileScreen(
                            nickname = nick,
                            avatarEmoji = avatar,
                            customAvatarUri = customUri,
                            themeIndex = themeIdx,
                            onNicknameChange = { profileViewModel.setNickname(it) },
                            onAvatarChange = { profileViewModel.setAvatarEmoji(it) },
                            onCustomAvatarChange = { profileViewModel.setCustomAvatarUri(it) },
                            onThemeChange = {
                                profileViewModel.setThemeIndex(it)
                                scope.launch { snackbarHostState.showSnackbar("主题已切换") }
                            },
                            onClearAllData = {
                                profileViewModel.clearAllData {
                                    scope.launch { snackbarHostState.showSnackbar("已清空全部记录") }
                                }
                            },


                            onNavigateToImport = { navController.navigate(MainRoute.IMPORT) },
                            aiChatEnabled = aiChatEnabled,
                            onAiChatToggle = { enabled ->
                                profileViewModel.setAiChatEnabled(enabled)
                                if (enabled) showAiChatProfileTutorial = true
                            },
                            customThemeConfig = customThemeCfg,
                            onCustomThemeChange = { profileViewModel.setCustomThemeConfig(it) },
                            triggerKeywords = triggerKw,
                            onAddTriggerKeyword = { profileViewModel.addTriggerKeyword(it) },
                            onRemoveTriggerKeyword = { profileViewModel.removeTriggerKeyword(it) },
                            customCategories = customCats,
                            onAddCustomCategory = { profileViewModel.addCustomCategory(it) },
                            onRemoveCustomCategory = { profileViewModel.removeCustomCategory(it) },
                            customAiPrompt = aiPrompt,
                            onCustomAiPromptChange = { profileViewModel.setCustomAiPrompt(it) },
                            onExportCsv = { start, end ->
                                scope.launch {
                                    val uri = profileViewModel.exportCsv(
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
                                    val uri = profileViewModel.exportImage(
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
                    composable(MainRoute.AI_CHAT) {
                        val aiNick by profileViewModel.aiNickname.collectAsStateWithLifecycle()
                        val aiAvatar by profileViewModel.aiAvatarEmoji.collectAsStateWithLifecycle()
                        val aiCustomUri by profileViewModel.aiCustomAvatarUri.collectAsStateWithLifecycle()
                        var showAiCustomizeDialog by remember { mutableStateOf(false) }

                        val aiImagePickerLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            uri?.let { profileViewModel.setAiCustomAvatarUri(it.toString()) }
                        }

                        AIChatScreen(
                            onBack = { navController.popBackStack() },
                            onAddTransaction = { amount, type, desc ->
                                ledgerViewModel.addTransaction(amount, type, desc) { success ->
                                    if (!success) scope.launch { snackbarHostState.showSnackbar("检测到重复账单，已跳过") }
                                }
                            },
                            viewModel = ledgerViewModel,
                            aiService = ledgerViewModel.aiBillService,
                            aiNickname = aiNick,
                            aiAvatarIndex = aiAvatar,
                            aiCustomAvatarUri = aiCustomUri,
                            onAiCustomizeClick = { showAiCustomizeDialog = true }
                        )

                        if (showAiCustomizeDialog) {
                            com.jizhang.tracker.ui.dialogs.AiCustomizeDialog(
                                currentNickname = aiNick,
                                currentAvatarIndex = aiAvatar,
                                customAvatarUri = aiCustomUri,
                                onImagePickerLaunch = { aiImagePickerLauncher.launch("image/*") },
                                onClearCustom = { profileViewModel.setAiCustomAvatarUri("") },
                                onConfirm = { name, index ->
                                    profileViewModel.setAiNickname(name)
                                    profileViewModel.setAiAvatarEmoji(index)
                                    showAiCustomizeDialog = false
                                },
                                onDismiss = { showAiCustomizeDialog = false }
                            )
                        }
                    }
                    composable(MainRoute.IMPORT) {
                        ImportScreen(
                            onBack = { navController.popBackStack() },
                            onImport = { bills -> ledgerViewModel.importBills(bills) { dup -> scope.launch { snackbarHostState.showSnackbar("已导入 ${bills.size - dup} 条账单" + if (dup > 0) "，跳过 $dup 条重复" else "") } } }
                        )
                    }
                    composable(MainRoute.SEARCH) {
                        val searchHist = ledgerViewModel.searchHistory.collectAsStateWithLifecycle().value
                        SearchScreen(
                            onBack = { navController.popBackStack() },
                            allTransactions = allTransactions,
                            searchHistory = searchHist,
                            onSearchHistoryChange = { ledgerViewModel.setSearchHistory(it) }
                        )
                    }
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

    // ── 首次启动介绍页 ──
    val isFirstLaunch by profileViewModel.isFirstLaunch.collectAsStateWithLifecycle()

    var showIntroDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) showIntroDialog = true
    }

    // ── 所有弹窗 ──
    MainScreenDialogs(
        showAddDialog = showAddDialog,
        onDismissAddDialog = { showAddDialog = false },
        onConfirmAdd = { amount, type, note ->
            ledgerViewModel.addTransaction(amount, type, note) { success ->
                if (!success) {
                    scope.launch { snackbarHostState.showSnackbar("检测到重复账单，已跳过") }
                }
            }
            showAddDialog = false
        },
        showAddPlanDialog = showAddPlanDialog,
        onDismissAddPlanDialog = { showAddPlanDialog = false },
        onConfirmPlan = { name, amount, note, type ->
            planViewModel.addCustomPlan(name, amount, note, type)
            showAddPlanDialog = false
        },
        showDateFilterDialog = showDateFilterDialog,
        initialFilterStart = filterStartDate,
        initialFilterEnd = filterEndDate,
        onDateFilterConfirm = { start, end ->
            ledgerViewModel.filterStartDate.value = start
            ledgerViewModel.filterEndDate.value = end
            showDateFilterDialog = false
        },
        onDateFilterReset = {
            ledgerViewModel.filterStartDate.value = null
            ledgerViewModel.filterEndDate.value = null
            showDateFilterDialog = false
        },
        onDismissDateFilter = { showDateFilterDialog = false },
        detailTransaction = detailTransaction,
        onDismissDetail = { detailTransaction = null },
        onNoteSave = { id, note -> ledgerViewModel.updateTransactionNote(id, note) },
        showManualPlanAlert = showManualPlanAlert,
        onDismissManualPlanAlert = { showManualPlanAlert = false },
        showAIChatTutorial = showAIChatTutorial,
        onDismissAIChatTutorial = {
            showAIChatTutorial = false
            profileViewModel.markAiChatTutorialDone()
            navController.navigate(MainRoute.AI_CHAT)
        },
        showAiChatProfileTutorial = showAiChatProfileTutorial,
        onDismissAiChatProfileTutorial = { showAiChatProfileTutorial = false },
        showIntroDialog = showIntroDialog,
        onDismissIntro = { profileViewModel.dismissFirstLaunch(); showIntroDialog = false },
        showUpdateDialog = showUpdateDialog,
        onDismissUpdate = { showUpdateDialog = false },
        showBackupDialog = showBackupDialog,
        onDismissBackup = { showBackupDialog = false },
        onExportBackup = {
            scope.launch {
                val uri = profileViewModel.exportBackup()
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
        onImportRestore = { backupImportLauncher.launch("application/json") }
    )
}




