package com.example.billtracker.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.ui.components.TagChip
import com.example.billtracker.ui.components.TransactionCard
import com.example.billtracker.viewmodel.AnalysisViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class TimeRange { DAY, WEEK, MONTH, YEAR, CUSTOM }

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val cal = remember { Calendar.getInstance() }
    val context = LocalContext.current

    var timeRange by remember { mutableStateOf(TimeRange.DAY) }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var customStart by remember { mutableStateOf<Long?>(null) }
    var customEnd by remember { mutableStateOf<Long?>(null) }

    val rangeStart = remember(timeRange, selectedYear, selectedMonth, customStart) {
        when (timeRange) {
            TimeRange.DAY -> {
                val c = Calendar.getInstance()
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            TimeRange.WEEK -> System.currentTimeMillis() - 6L * 86400000L
            TimeRange.MONTH -> {
                val c = Calendar.getInstance()
                c.set(selectedYear, selectedMonth, 1, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            TimeRange.YEAR -> {
                val c = Calendar.getInstance()
                c.set(selectedYear, 0, 1, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }
            TimeRange.CUSTOM -> customStart ?: 0L
        }
    }
    val rangeEnd = remember(timeRange, selectedYear, selectedMonth, customEnd) {
        when (timeRange) {
            TimeRange.DAY -> {
                val c = Calendar.getInstance()
                c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
                c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
                c.timeInMillis
            }
            TimeRange.WEEK -> System.currentTimeMillis()
            TimeRange.MONTH -> {
                val c = Calendar.getInstance()
                c.set(selectedYear, selectedMonth, 1, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                c.add(Calendar.MONTH, 1)
                c.timeInMillis - 1
            }
            TimeRange.YEAR -> {
                val c = Calendar.getInstance()
                c.set(selectedYear + 1, 0, 1, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis - 1
            }
            TimeRange.CUSTOM -> customEnd ?: System.currentTimeMillis()
        }
    }

    val filteredTransactions = remember(allTransactions, rangeStart, rangeEnd) {
        allTransactions.filter { it.dateMillis in rangeStart..rangeEnd }
    }

    val totalIncome = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val categoryExpenses = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    val dailyGroups = remember(filteredTransactions) {
        filteredTransactions.sortedByDescending { it.dateMillis }
            .groupBy { tx ->
                val c = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                c.get(Calendar.YEAR) to c.get(Calendar.DAY_OF_YEAR)
            }
            .entries.sortedByDescending { it.key.first * 1000 + it.key.second }
    }

    fun goPrev() {
        when (timeRange) {
            TimeRange.MONTH -> {
                if (selectedMonth == 0) { selectedYear--; selectedMonth = 11 }
                else selectedMonth--
            }
            TimeRange.YEAR -> selectedYear--
            else -> {}
        }
    }

    fun goNext() {
        when (timeRange) {
            TimeRange.MONTH -> {
                if (selectedMonth == 11) { selectedYear++; selectedMonth = 0 }
                else selectedMonth++
            }
            TimeRange.YEAR -> selectedYear++
            else -> {}
        }
    }

    val catColors = remember {
        listOf(
            Color(0xFFE8824A), Color(0xFF4CAF7A), Color(0xFF5B8DB8), Color(0xFFD4A06A),
            Color(0xFFD4889A), Color(0xFF9B7BB5), Color(0xFF6BB5B5), Color(0xFFE8A06A),
            Color(0xFFA0A0A0), Color(0xFF7AB87A)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── 时间范围选择 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.entries.forEach { range ->
                TagChip(
                    selected = timeRange == range,
                    onClick = { timeRange = range },
                    label = when (range) {
                        TimeRange.DAY -> "日"
                        TimeRange.WEEK -> "周"
                        TimeRange.MONTH -> "月"
                        TimeRange.YEAR -> "年"
                        TimeRange.CUSTOM -> "自定义"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── 时间导航 + 概要统计 ──
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { goPrev() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一个",
                            tint = if (timeRange in listOf(TimeRange.DAY, TimeRange.MONTH, TimeRange.YEAR))
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else Color.Transparent,
                            modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = when (timeRange) {
                            TimeRange.DAY -> {
                                val fmt = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
                                fmt.format(Date(System.currentTimeMillis()))
                            }
                            TimeRange.WEEK -> "最近7天"
                            TimeRange.MONTH -> "${selectedYear}年${selectedMonth + 1}月"
                            TimeRange.YEAR -> "${selectedYear}年"
                            TimeRange.CUSTOM -> {
                                val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
                                val s = customStart?.let { fmt.format(Date(it)) } ?: "开始"
                                val e = customEnd?.let { fmt.format(Date(it)) } ?: "结束"
                                "$s - $e"
                            }
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable(enabled = timeRange == TimeRange.CUSTOM) {
                                val now = Calendar.getInstance()
                                val dpd = DatePickerDialog(
                                    context, { _, y, m, d ->
                                        val c = Calendar.getInstance()
                                        c.set(y, m, d, 0, 0, 0)
                                        c.set(Calendar.MILLISECOND, 0)
                                        customStart = c.timeInMillis
                                        c.add(Calendar.DAY_OF_MONTH, 30)
                                        customEnd = c.timeInMillis
                                    },
                                    now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
                                )
                                dpd.show()
                            }
                    )
                    IconButton(onClick = { goNext() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一个",
                            tint = if (timeRange in listOf(TimeRange.MONTH, TimeRange.YEAR))
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else Color.Transparent,
                            modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem("收入", totalIncome, IncomeGreen)
                    SummaryItem("支出", totalExpense, ExpenseRed)
                    SummaryItem("结余", netBalance, if (netBalance >= 0) IncomeGreen else ExpenseRed)
                }

                if (totalIncome > 0 || totalExpense > 0) {
                    Spacer(Modifier.height(12.dp))
                    val ratio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 1f
                    val expensePct = (ratio * 100).toInt()
                    val incomePct = 100 - expensePct
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DividerColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .background(ExpenseRed, RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "支出${expensePct}% | 收入${incomePct}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 每日明细（多日视图显示） ──
        if (timeRange != TimeRange.DAY && dailyGroups.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("每日明细", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    dailyGroups.forEach { (_, txs) ->
                        val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        val firstTx = txs.first()
                        val dateLabel = remember(firstTx.dateMillis) {
                            val c = Calendar.getInstance().apply { timeInMillis = firstTx.dateMillis }
                            val fmt = SimpleDateFormat("MM月dd日", Locale.getDefault())
                            val dow = when (c.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> "周一"; Calendar.TUESDAY -> "周二"
                                Calendar.WEDNESDAY -> "周三"; Calendar.THURSDAY -> "周四"
                                Calendar.FRIDAY -> "周五"; Calendar.SATURDAY -> "周六"
                                Calendar.SUNDAY -> "周日"; else -> ""
                            }
                            "${fmt.format(Date(firstTx.dateMillis))} $dow"
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(dateLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            if (dayExpense > 0) Text("支出 ¥%.2f".format(dayExpense), fontSize = 13.sp, color = ExpenseRed, fontWeight = FontWeight.Medium)
                            if (dayIncome > 0 && dayExpense > 0) Text("  ", fontSize = 13.sp)
                            if (dayIncome > 0) Text("收入 ¥%.2f".format(dayIncome), fontSize = 13.sp, color = IncomeGreen, fontWeight = FontWeight.Medium)
                        }
                        txs.sortedByDescending { it.dateMillis }.forEach { tx -> AnalysisTransactionRow(tx = tx) }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 折线图（日视图不显示） ──
        if (timeRange != TimeRange.DAY) {
            Spacer(Modifier.height(16.dp))
            ChartCard(title = "折线图") {
                LineChart(
                    transactions = filteredTransactions,
                    incomeColor = IncomeGreen,
                    expenseColor = ExpenseRed
                )
            }

            Spacer(Modifier.height(16.dp))
            ChartCard(title = "柱状图") {
                BarChart(
                    transactions = filteredTransactions,
                    incomeColor = IncomeGreen,
                    expenseColor = ExpenseRed
                )
            }
        }

        // ── 饼图 ──
        Spacer(Modifier.height(16.dp))
        ChartCard(title = "饼图") {
            PieChart(
                categoryExpenses = categoryExpenses, catColors = catColors,
                totalIncome = totalIncome, totalExpense = totalExpense
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── 支出分类明细（柱状进度条） ──
        if (categoryExpenses.isNotEmpty()) {
            val maxCatAmount = categoryExpenses.first().value.coerceAtLeast(1.0)
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "支出分类",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    categoryExpenses.forEachIndexed { i, (cat, amount) ->
                        val pct = amount / totalExpense.coerceAtLeast(1.0) * 100
                        val barFraction = (amount / maxCatAmount).toFloat().coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(DividerColor)) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(barFraction)
                                    .background(catColors[i % catColors.size], RoundedCornerShape(3.dp)))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("¥%.1f".format(amount), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                            Spacer(Modifier.width(4.dp))
                            Text("%.1f%%".format(pct), fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── AI 月账单总结 ──
        if (filteredTransactions.isNotEmpty()) {
            var summaryText by remember { mutableStateOf<String?>(null) }
            var summaryLoading by remember { mutableStateOf(false) }
            var summaryError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(timeRange, selectedYear, selectedMonth, customStart, customEnd) {
                summaryText = null
                summaryError = null
                if (filteredTransactions.isEmpty()) return@LaunchedEffect
                summaryLoading = true
                try {
                    val yearLabel = when (timeRange) {
                        TimeRange.DAY -> Calendar.getInstance().get(Calendar.YEAR)
                        TimeRange.MONTH -> selectedYear
                        TimeRange.YEAR -> selectedYear
                        TimeRange.WEEK -> Calendar.getInstance().get(Calendar.YEAR)
                        TimeRange.CUSTOM -> Calendar.getInstance().get(Calendar.YEAR)
                    }
                    val monthLabel = when (timeRange) {
                        TimeRange.DAY -> Calendar.getInstance().get(Calendar.MONTH) + 1
                        TimeRange.MONTH -> selectedMonth + 1
                        TimeRange.YEAR -> -1
                        TimeRange.WEEK -> -1
                        TimeRange.CUSTOM -> -1
                    }
                    val catMap = categoryExpenses.associate { it.key to it.value }
                    summaryText = viewModel.generateMonthlySummary(
                        year = yearLabel,
                        month = monthLabel,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        netBalance = netBalance,
                        expenseCategories = catMap,
                        transactionCount = filteredTransactions.size
                    )
                } catch (e: Exception) {
                    summaryError = e.message ?: "请求失败"
                } finally {
                    summaryLoading = false
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("AI 月账单总结", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(10.dp))
                    when {
                        summaryLoading -> {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("AI 正在分析你的账单...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        summaryError != null -> {
                            Text(summaryError!!, fontSize = 13.sp, color = ExpenseRed, lineHeight = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                summaryText = null; summaryError = null; summaryLoading = true
                            }) {
                                Text("重试", fontSize = 13.sp)
                            }
                        }
                        summaryText != null -> {
                            Text(summaryText!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── 数据辅助类 ──
private data class ChartLabel(val label: String, val amount: Double, val isIncome: Boolean)

// ── 折线图（每笔账单一个点，可交互） ──
@Composable
private fun LineChart(
    transactions: List<TransactionEntity>,
    incomeColor: Color,
    expenseColor: Color
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    val sorted = remember(transactions) { transactions.sortedBy { it.dateMillis } }
    val dataPoints = remember(sorted) {
        val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
        sorted.map { tx ->
            ChartLabel(fmt.format(Date(tx.dateMillis)), tx.amount, tx.type == TransactionType.INCOME)
        }
    }

    if (dataPoints.isEmpty()) return

    val maxVal = dataPoints.maxOf { it.amount.coerceAtLeast(1.0) }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val animProgress by animateFloatAsState(
        targetValue = if (selectedIndex != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "lineAnim"
    )

    val density = LocalDensity.current.density
    val baseItemW = 64f
    val itemWd = baseItemW * zoomScale

    var viewportW by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(selectedIndex, zoomScale, viewportW) {
        if (selectedIndex != null && viewportW > 0) {
            val idx = selectedIndex!!
            val pointCenterDp = 24f + idx * itemWd + itemWd / 2
            val targetScroll = ((pointCenterDp * density - viewportW / 2).toInt()).coerceAtLeast(0)
            scrollState.animateScrollTo(targetScroll)
        }
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("收入", incomeColor); LegendItem("支出", expenseColor)
        }
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .onSizeChanged { viewportW = it.width.toFloat() }
        ) {
            val minContentDp = maxOf(viewportW / density, dataPoints.size * itemWd + 48f)
            Canvas(
                modifier = Modifier
                    .width(minContentDp.dp)
                    .height(200.dp)
                    .pointerInput(dataPoints, zoomScale) {
                        detectTapGestures { offset ->
                            val curItemWd = baseItemW * zoomScale
                            val tapIdx = ((offset.x / density - 24f) / curItemWd).toInt()
                                .coerceIn(0, dataPoints.lastIndex)
                            selectedIndex = if (selectedIndex == tapIdx) null else tapIdx
                        }
                    }
            ) {
                val chartL = 24f.dp.toPx()
                val chartT = 36f.dp.toPx()
                val chartB = size.height - 32f.dp.toPx()
                val chartH = (chartB - chartT).coerceAtLeast(1f)
                val itemW = itemWd.dp.toPx()

                val datePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(
                        (labelColor.alpha * 255).toInt(), (labelColor.red * 255).toInt(),
                        (labelColor.green * 255).toInt(), (labelColor.blue * 255).toInt())
                    textSize = 11f.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER
                }

                dataPoints.forEachIndexed { i, dp ->
                    val cx = chartL + i * itemW + itemW / 2
                    val y = chartT + chartH - (dp.amount / maxVal * chartH).toFloat().coerceIn(0f, chartH)
                    val color = if (dp.isIncome) incomeColor else expenseColor

                    if (i > 0) {
                        val prev = dataPoints[i - 1]
                        val prevCx = chartL + (i - 1) * itemW + itemW / 2
                        val prevY = chartT + chartH - (prev.amount / maxVal * chartH).toFloat().coerceIn(0f, chartH)
                        drawLine(color.copy(alpha = 0.4f), Offset(prevCx, prevY), Offset(cx, y), strokeWidth = 2f)
                    }

                    val isSelected = selectedIndex == i
                    val magnify = 1f + animProgress * 1.0f
                    val labelSize = if (isSelected) (10f * magnify).dp.toPx() else 10f.dp.toPx()
                    val amtPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.argb(
                            (color.alpha * 255).toInt(), (color.red * 255).toInt(),
                            (color.green * 255).toInt(), (color.blue * 255).toInt())
                        textSize = labelSize; textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "¥${"%.0f".format(dp.amount)}", cx, y - 8f.dp.toPx(), amtPaint)

                    val pointRadius = 4f.dp.toPx() * magnify
                    if (isSelected && animProgress > 0.01f) {
                        drawCircle(color.copy(alpha = 0.2f),
                            pointRadius + 4f.dp.toPx(), Offset(cx, y))
                    }
                    drawCircle(color, pointRadius, Offset(cx, y))

                    val labelStep = maxOf(dataPoints.size / 10, 1)
                    if (i % labelStep == 0 || i == dataPoints.lastIndex) {
                        drawContext.canvas.nativeCanvas.drawText(dp.label, cx, chartB + 24f.dp.toPx(), datePaint)
                    }
                }
            }
        }

        // 缩放控制
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { zoomScale = (zoomScale / 1.5f).coerceAtLeast(0.3f) }) {
                Text("缩小", fontSize = 11.sp)
            }
            TextButton(onClick = { zoomScale = 1f; selectedIndex = null }) {
                Text("重置", fontSize = 11.sp)
            }
            TextButton(onClick = { zoomScale = (zoomScale * 1.5f).coerceAtMost(4f) }) {
                Text("放大", fontSize = 11.sp)
            }
        }
    }
}

// ── 柱状图（每笔账单一根柱子，可交互） ──
@Composable
private fun BarChart(
    transactions: List<TransactionEntity>,
    incomeColor: Color,
    expenseColor: Color
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    val sorted = remember(transactions) { transactions.sortedBy { it.dateMillis } }
    val dataPoints = remember(sorted) {
        val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
        sorted.map { tx ->
            ChartLabel(fmt.format(Date(tx.dateMillis)), tx.amount, tx.type == TransactionType.INCOME)
        }
    }

    if (dataPoints.isEmpty()) return

    val maxVal = dataPoints.maxOf { it.amount.coerceAtLeast(1.0) }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val animProgress by animateFloatAsState(
        targetValue = if (selectedIndex != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "barAnim"
    )

    val density = LocalDensity.current.density
    val baseItemW = 56f
    val itemWd = baseItemW * zoomScale

    var viewportW by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(selectedIndex, zoomScale, viewportW) {
        if (selectedIndex != null && viewportW > 0) {
            val idx = selectedIndex!!
            val pointCenterDp = 24f + idx * itemWd + itemWd / 2
            val targetScroll = ((pointCenterDp * density - viewportW / 2).toInt()).coerceAtLeast(0)
            scrollState.animateScrollTo(targetScroll)
        }
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("收入", incomeColor); LegendItem("支出", expenseColor)
        }
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .onSizeChanged { viewportW = it.width.toFloat() }
        ) {
            val minContentDp = maxOf(viewportW / density, dataPoints.size * itemWd + 48f)
            Canvas(
                modifier = Modifier
                    .width(minContentDp.dp)
                    .height(200.dp)
                    .pointerInput(dataPoints, zoomScale) {
                        detectTapGestures { offset ->
                            val curItemWd = baseItemW * zoomScale
                            val tapIdx = ((offset.x / density - 24f) / curItemWd).toInt()
                                .coerceIn(0, dataPoints.lastIndex)
                            selectedIndex = if (selectedIndex == tapIdx) null else tapIdx
                        }
                    }
            ) {
                val chartL = 24f.dp.toPx()
                val chartT = 36f.dp.toPx()
                val chartB = size.height - 32f.dp.toPx()
                val chartH = (chartB - chartT).coerceAtLeast(1f)
                val itemW = itemWd.dp.toPx()
                val barW = itemW * 0.5f
                val leftGap = itemW * 0.25f

                val datePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(
                        (labelColor.alpha * 255).toInt(), (labelColor.red * 255).toInt(),
                        (labelColor.green * 255).toInt(), (labelColor.blue * 255).toInt())
                    textSize = 11f.dp.toPx(); textAlign = android.graphics.Paint.Align.CENTER
                }

                dataPoints.forEachIndexed { i, dp ->
                    val x = chartL + i * itemW + leftGap
                    val h = (dp.amount / maxVal * chartH).toFloat().coerceAtLeast(0f)
                    val y = chartT + chartH - h
                    val color = if (dp.isIncome) incomeColor else expenseColor

                    val isSelected = selectedIndex == i
                    val magnify = 1f + animProgress * 1.0f
                    val labelSize = if (isSelected) (10f * magnify).dp.toPx() else 10f.dp.toPx()
                    val amtPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.argb(
                            (color.alpha * 255).toInt(), (color.red * 255).toInt(),
                            (color.green * 255).toInt(), (color.blue * 255).toInt())
                        textSize = labelSize; textAlign = android.graphics.Paint.Align.CENTER
                    }
                    if (h > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "¥${"%.0f".format(dp.amount)}", x + barW / 2, y - 6f.dp.toPx(), amtPaint)
                    }

                    val extraW = 6f.dp.toPx() * magnify
                    val sw = barW + extraW
                    drawRect(color.copy(alpha = if (isSelected) 1f else 0.85f),
                        topLeft = Offset(x - extraW / 2, y), size = Size(sw, h))

                    if (isSelected && animProgress > 0.01f) {
                        drawRect(color.copy(alpha = 0.2f),
                            topLeft = Offset(x - extraW / 2 - 2f.dp.toPx(), y - 2f.dp.toPx()),
                            size = Size(sw + 4f.dp.toPx(), h + 4f.dp.toPx()),
                            style = Stroke(width = 2f.dp.toPx()))
                    }

                    val labelStep = maxOf(dataPoints.size / 10, 1)
                    if (i % labelStep == 0 || i == dataPoints.lastIndex) {
                        drawContext.canvas.nativeCanvas.drawText(dp.label, x + barW / 2, chartB + 24f.dp.toPx(), datePaint)
                    }
                }
            }
        }

        // 缩放控制
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { zoomScale = (zoomScale / 1.5f).coerceAtLeast(0.3f) }) {
                Text("缩小", fontSize = 11.sp)
            }
            TextButton(onClick = { zoomScale = 1f; selectedIndex = null }) {
                Text("重置", fontSize = 11.sp)
            }
            TextButton(onClick = { zoomScale = (zoomScale * 1.5f).coerceAtMost(4f) }) {
                Text("放大", fontSize = 11.sp)
            }
        }
    }
}

// ── 饼图（收入占比 + 支出分类） ──
@Composable
private fun PieChart(
    categoryExpenses: List<Map.Entry<String, Double>>,
    catColors: List<Color>,
    totalIncome: Double,
    totalExpense: Double
) {
    val total = categoryExpenses.sumOf { it.value }.coerceAtLeast(1.0)
    val sorted = categoryExpenses

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip("收入", "¥%.2f".format(totalIncome), IncomeGreen)
            StatChip("支出", "¥%.2f".format(totalExpense), ExpenseRed)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val strokeWidth = 40f
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                if (sorted.isEmpty()) {
                    drawArc(
                        color = Color(0xFFE0E0E0),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2), style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f
                    sorted.forEachIndexed { i, (_, amount) ->
                        val sweep = (amount.toFloat() / total.toFloat() * 360f).coerceAtLeast(0.5f)
                        drawArc(
                            color = catColors[i % catColors.size],
                            startAngle = startAngle, sweepAngle = sweep, useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2), style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweep
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                sorted.forEachIndexed { i, (cat, amount) ->
                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(catColors[i % catColors.size], RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text(cat, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("%.1f%%".format(amount / total * 100), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SummaryItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(4.dp))
        Text("¥%.2f".format(amount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AnalysisTransactionRow(tx: TransactionEntity) {
    TransactionCard(transaction = tx, compactMode = true)
}
