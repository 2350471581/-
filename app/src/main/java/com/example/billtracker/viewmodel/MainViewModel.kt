package com.example.billtracker.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.NotificationListener
import com.example.billtracker.data.SmsParser
import com.example.billtracker.data.SmsReceiver
import com.example.billtracker.data.SmsMessage
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionRepository
import com.example.billtracker.data.TransactionType
import com.example.billtracker.data.ParsedBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.billtracker.data.PlanStorage
import com.example.billtracker.data.CustomPlan
import com.example.billtracker.data.PlanDataType
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.ui.CustomThemeConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(application)

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val todayEnd: Long
        get() = todayStart + 86400000L

    val todayTransactions: StateFlow<List<TransactionEntity>> =
        repository.getTodayTransactions(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayIncome: StateFlow<Double> =
        repository.getTodayIncome(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExpense: StateFlow<Double> =
        repository.getTodayExpense(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRefreshing = MutableStateFlow(false)

    // ── 计划数据 ──
    private val planStorage = PlanStorage(application)
    val installDateMillis = planStorage.installDateMillis

    // 全部记录：默认显示最近30天，可选择日期范围过滤
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    val recentTransactions: StateFlow<List<TransactionEntity>> =
        combine(allTransactions, filterStartDate, filterEndDate) { list, start, end ->
            list.filter { it.dateMillis >= installDateMillis }.let { filtered ->
                val s = start ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                val e = end ?: Long.MAX_VALUE
                filtered.filter { it.dateMillis in s..e }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val planBalance = MutableStateFlow(planStorage.balance)
    val todayPlanTarget = MutableStateFlow(planStorage.todayPlanTarget)
    val totalPlanTarget = MutableStateFlow(planStorage.totalPlanTarget)
    val savePlanTarget = MutableStateFlow(planStorage.savePlanTarget)
    val todayPlanNote = MutableStateFlow(planStorage.todayPlanNote)
    val totalPlanNote = MutableStateFlow(planStorage.totalPlanNote)
    val savePlanNote = MutableStateFlow(planStorage.savePlanNote)

    // ── 自定义计划数据 ──
    val customPlans = MutableStateFlow(planStorage.getAllCustomPlans())

    // ── 手动/自动模式 ──
    val isManualMode = MutableStateFlow(planStorage.isManualMode)

    // ── AI 聊天式记账 ──
    val aiChatEnabled = MutableStateFlow(planStorage.aiChatEnabled)
    val aiChatTutorialDone = MutableStateFlow(planStorage.aiChatTutorialDone)

    fun setAiChatEnabled(enabled: Boolean) {
        aiChatEnabled.value = enabled
        planStorage.aiChatEnabled = enabled
    }

    fun markAiChatTutorialDone() {
        aiChatTutorialDone.value = true
        planStorage.aiChatTutorialDone = true
    }

    // ── 通知监听权限 ──
    val hasNotificationPermission = MutableStateFlow(
        NotificationListener.isPermissionGranted(application)
    )

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun checkNotificationPermission() {
        hasNotificationPermission.value = NotificationListener.isPermissionGranted(getApplication())
    }

    // ── 首次启动弹窗 ──
    val isFirstLaunch = MutableStateFlow(planStorage.isFirstLaunch)

    init {
        // 注册短信接收回调（手动模式下不处理）
        SmsReceiver.SmsReceiverCallback = object : SmsReceiver.SmsReceiverCallback {
            override fun onNewTransaction(sms: SmsMessage, result: SmsParser.ParseResult) {
                if (isManualMode.value) return // 手动模式不自动添加
                viewModelScope.launch {
                    repository.insertFromReceiver(sms, result)
                }
            }
        }
    }

    fun setManualMode(enabled: Boolean) {
        isManualMode.value = enabled
        planStorage.isManualMode = enabled
    }

    fun addTransaction(amount: Double, type: TransactionType, description: String) {
        viewModelScope.launch {
            repository.addManualTransaction(amount, type, description)
        }
    }

    fun importBills(bills: List<ParsedBill>) {
        viewModelScope.launch {
            bills.forEach { repository.importTransaction(it) }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateTransactionNote(id: Long, note: String) {
        viewModelScope.launch {
            repository.updateTransactionDescription(id, note)
        }
    }

    fun addTestTransaction() {
        viewModelScope.launch {
            val testData = arrayOf(
                Triple("微信支付", "收款", TransactionType.INCOME),
                Triple("微信支付", "消费", TransactionType.EXPENSE),
                Triple("微信支付", "付款", TransactionType.EXPENSE),
                Triple("微信支付", "退款", TransactionType.INCOME),
                Triple("支付宝", "收入", TransactionType.INCOME),
                Triple("支付宝", "向你收款", TransactionType.EXPENSE),
                Triple("支付宝", "向你付款", TransactionType.INCOME),
                Triple("支付宝", "消费", TransactionType.EXPENSE),
            )
            val (sender, action, type) = testData.random()
            val amount = (1..100).random().toDouble()
            val source = if (sender.contains("微信")) TransactionSource.WECHAT else TransactionSource.ALIPAY
            val body = "【$sender】${action}¥${"%.2f".format(amount)}"

            repository.insertFromReceiver(
                SmsMessage(System.currentTimeMillis(), body, System.currentTimeMillis()),
                SmsParser.ParseResult(amount, type, source)
            )
        }
    }

    fun refreshFromSms() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.importFromSms()
            isRefreshing.value = false
        }
    }

    // ── 计划方法 ──
    fun updatePlanBalance(balance: Double) {
        planBalance.value = balance
        planStorage.balance = balance
    }

    fun updateTodayPlanTarget(target: Double) {
        todayPlanTarget.value = target
        planStorage.todayPlanTarget = target
    }

    fun updateTotalPlanTarget(target: Double) {
        totalPlanTarget.value = target
        planStorage.totalPlanTarget = target
    }

    fun updateTodayPlanNote(note: String) {
        todayPlanNote.value = note
        planStorage.todayPlanNote = note
    }

    fun updateTotalPlanNote(note: String) {
        totalPlanNote.value = note
        planStorage.totalPlanNote = note
    }

    fun updateSavePlanTarget(target: Double) {
        savePlanTarget.value = target
        planStorage.savePlanTarget = target
    }

    fun updateSavePlanNote(note: String) {
        savePlanNote.value = note
        planStorage.savePlanNote = note
    }

    // ── 首次启动弹窗 ──
    fun dismissFirstLaunch() {
        planStorage.markFirstLaunchSeen()
        isFirstLaunch.value = false
    }

    // ── 自定义计划方法 ──
    fun addCustomPlan(name: String, target: Double, note: String, type: PlanDataType = PlanDataType.TODAY_NET) {
        val plan = CustomPlan(name, target, note, type)
        planStorage.addCustomPlan(plan)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    fun updateCustomPlan(index: Int, target: Double, note: String) {
        planStorage.updateCustomPlan(index, target, note)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    fun deleteCustomPlan(index: Int) {
        planStorage.deleteCustomPlan(index)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    // ── 主题 / 昵称 / 头像 ──
    val themeIndex = MutableStateFlow(planStorage.themeIndex)
    val followSystemTheme = MutableStateFlow(planStorage.followSystemTheme)
    val nickname = MutableStateFlow(planStorage.nickname)
    val avatarEmoji = MutableStateFlow(planStorage.avatarEmoji)
    val customAvatarUri = MutableStateFlow(planStorage.customAvatarUri)

    fun setThemeIndex(index: Int) {
        themeIndex.value = index
        planStorage.themeIndex = index
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        followSystemTheme.value = enabled
        planStorage.followSystemTheme = enabled
    }

    fun setNickname(name: String) {
        nickname.value = name
        planStorage.nickname = name
    }

    fun setAvatarEmoji(index: Int) {
        avatarEmoji.value = index
        planStorage.avatarEmoji = index
    }

    fun setCustomAvatarUri(uri: String) {
        customAvatarUri.value = uri
        planStorage.customAvatarUri = uri
    }

    // ── 自定义主题 ──
    val customThemeConfig = MutableStateFlow(
        CustomThemeConfig.fromJson(planStorage.customThemeConfigJson)
    )

    fun setCustomThemeConfig(config: CustomThemeConfig) {
        customThemeConfig.value = config
        planStorage.customThemeConfigJson = config.toJson()
    }

    // ── 清空全部数据 ──
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // ── 账单分析数据 ──
    data class AnalysisPeriod(val label: String, val startMillis: Long)

    fun getAnalysisPeriods(): List<AnalysisPeriod> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        return listOf(
            AnalysisPeriod("今天", run {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }),
            AnalysisPeriod("昨天", run {
                cal.timeInMillis = now - 86400000L
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }),
            AnalysisPeriod("一周", run { now - 7L * 86400000L }),
            AnalysisPeriod("一个月", run { now - 30L * 86400000L }),
            AnalysisPeriod("三个月", run { now - 90L * 86400000L }),
            AnalysisPeriod("半年", run { now - 180L * 86400000L }),
        )
    }

    suspend fun getAnalysisTransactions(startMillis: Long): List<TransactionEntity> {
        return withContext(Dispatchers.IO) {
            dao.getTransactionsBetweenSync(startMillis, System.currentTimeMillis())
        }
    }

    // ── 导出 CSV ──
    suspend fun exportCsv(startMillis: Long = 0, endMillis: Long = System.currentTimeMillis()): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTx = dao.getTransactionsBetweenSync(startMillis, endMillis)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val csv = buildString {
                appendLine("日期,金额,类型,来源,分类,备注")
                allTx.forEach { tx ->
                    val type = if (tx.type == TransactionType.INCOME) "收入" else "支出"
                    val source = when (tx.source) {
                        TransactionSource.WECHAT -> "微信"
                        TransactionSource.ALIPAY -> "支付宝"
                        TransactionSource.MANUAL -> "手动"
                        TransactionSource.BANK -> "银行"
                    }
                    appendLine("${dateFmt.format(tx.dateMillis)},${tx.amount},$type,$source,${tx.category},${tx.description.replace(",", "，")}")
                }
            }
            val file = File(getApplication<Application>().cacheDir, "billtracker_export.csv")
            file.writeText(csv)
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    // ── 导出图片 ──
    suspend fun exportImage(startMillis: Long = 0, endMillis: Long = System.currentTimeMillis()): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTx = dao.getTransactionsBetweenSync(startMillis, endMillis)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val width = 1080
            val lineHeight = 52
            val headerHeight = 160
            val rowHeight = 44
            val padding = 60
            val titleHeight = 100
            val summaryHeight = 120
            val totalRows = allTx.size.coerceAtLeast(1)
            val height = padding + titleHeight + summaryHeight + headerHeight + totalRows * rowHeight + padding

            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2C241A")
                textSize = 48f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#9AA0A6")
                textSize = 26f
            }
            val rowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#3C4043")
                textSize = 30f
            }
            val incomePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#4CAF7A")
                textSize = 30f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val expensePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EA6B5C")
                textSize = 30f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F1F3F4")
                strokeWidth = 1f
            }
            val grayPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#9AA0A6")
                textSize = 24f
            }

            var y = padding

            // 标题
            canvas.drawText("账单记录", 60f, y + 50f, titlePaint)
            y += titleHeight

            // 概要统计
            val totalIncome = allTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = allTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            canvas.drawText("收入: ¥${"%.2f".format(totalIncome)}", 60f, y.toFloat(), incomePaint)
            canvas.drawText("支出: ¥${"%.2f".format(totalExpense)}", 400f, y.toFloat(), expensePaint)
            y += summaryHeight

            // 表头
            canvas.drawRect(60f, y.toFloat(), (width - 60).toFloat(), (y + headerHeight).toFloat(), android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFF8F0")
            })
            canvas.drawText("日期", 80f, y + 36f, headerPaint)
            canvas.drawText("金额", 340f, y + 36f, headerPaint)
            canvas.drawText("分类", 540f, y + 36f, headerPaint)
            canvas.drawText("来源", 720f, y + 36f, headerPaint)
            canvas.drawText("备注", 860f, y + 36f, headerPaint)
            y += headerHeight

            // 数据行
            allTx.take(50).forEachIndexed { i, tx ->
                if (i % 2 == 1) {
                    canvas.drawRect(60f, y.toFloat(), (width - 60).toFloat(), (y + rowHeight).toFloat(), android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#F8F9FA")
                    })
                }

                val amountPaint = if (tx.type == TransactionType.INCOME) incomePaint else expensePaint
                val sign = if (tx.type == TransactionType.INCOME) "+" else "-"

                canvas.drawText(dateFmt.format(tx.dateMillis), 80f, y + 30f, grayPaint)
                canvas.drawText("${sign}¥${"%.2f".format(tx.amount)}", 340f, y + 30f, amountPaint)
                canvas.drawText(tx.category, 540f, y + 30f, rowPaint)

                val srcText = when (tx.source) {
                    TransactionSource.WECHAT -> "微信"
                    TransactionSource.ALIPAY -> "支付宝"
                    TransactionSource.MANUAL -> "手动"
                    TransactionSource.BANK -> "银行"
                }
                canvas.drawText(srcText, 720f, y + 30f, grayPaint)

                val desc = tx.description.take(8)
                canvas.drawText(desc, 860f, y + 30f, rowPaint)

                y += rowHeight
                canvas.drawLine(60f, y.toFloat(), (width - 60).toFloat(), y.toFloat(), linePaint)
            }

            if (allTx.size > 50) {
                canvas.drawText("... 共${allTx.size}条记录", 60f, y + 40f, grayPaint)
            }

            val file = File(getApplication<Application>().cacheDir, "billtracker_export.png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    private val dao = com.example.billtracker.data.AppDatabase.getInstance(getApplication()).transactionDao()

    suspend fun getTodaySummary(): String = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val end = start + 86400000L

        val tx = dao.getTransactionsBetweenSync(start, end)
        val income = tx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = tx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val count = tx.size

        buildString {
            appendLine("今日账单概况（截至${"HH:mm".let { java.text.SimpleDateFormat(it, Locale.getDefault()).format(java.util.Date()) }}）：")
            appendLine("收入：¥${"%.2f".format(income)}")
            appendLine("支出：¥${"%.2f".format(expense)}")
            appendLine("交易笔数：$count")
            if (tx.isNotEmpty()) {
                append("最近几笔：")
                tx.take(5).forEachIndexed { i, t ->
                    val type = if (t.type == TransactionType.INCOME) "收入" else "支出"
                    append("${if (i > 0) "；" else ""}${t.description} ${type}¥${"%.2f".format(t.amount)}")
                }
            }
        }
    }
}
