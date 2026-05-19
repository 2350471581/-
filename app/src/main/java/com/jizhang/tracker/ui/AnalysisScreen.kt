package com.jizhang.tracker.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jizhang.tracker.R
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.ui.components.TagChip
import com.jizhang.tracker.ui.components.TransactionCard
import com.jizhang.tracker.viewmodel.AnalysisViewModel
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
            TimeRange.DAY -> Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TimeRange.WEEK -> System.currentTimeMillis() - 6L * 86400000L
            TimeRange.MONTH -> Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TimeRange.YEAR -> Calendar.getInstance().apply {
                set(selectedYear, 0, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TimeRange.CUSTOM -> customStart ?: 0L
        }
    }
    val rangeEnd = remember(timeRange, selectedYear, selectedMonth, customEnd) {
        when (timeRange) {
            TimeRange.DAY -> Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            TimeRange.WEEK -> System.currentTimeMillis()
            TimeRange.MONTH -> Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1)
            }.timeInMillis - 1
            TimeRange.YEAR -> Calendar.getInstance().apply {
                set(selectedYear + 1, 0, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis - 1
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

    // 下钻状态
    var drillDownTitle by remember { mutableStateOf("") }
    var drillDownTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    val showDrillDown = drillDownTransactions.isNotEmpty()

    fun goPrev() {
        when (timeRange) {
            TimeRange.MONTH -> if (selectedMonth == 0) { selectedYear--; selectedMonth = 11 } else selectedMonth--
            TimeRange.YEAR -> selectedYear--
            else -> {}
        }
    }

    fun goNext() {
        when (timeRange) {
            TimeRange.MONTH -> if (selectedMonth == 11) { selectedYear++; selectedMonth = 0 } else selectedMonth++
            TimeRange.YEAR -> selectedYear++
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        TimeRangeSelector(
            timeRange = timeRange,
            onTimeRangeChange = { timeRange = it }
        )

        AnalysisSummaryCard(
            timeRange = timeRange,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            customStart = customStart,
            customEnd = customEnd,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            context = context,
            onPrev = { goPrev() },
            onNext = { goNext() },
            onCustomRangeSelected = { s, e -> customStart = s; customEnd = e }
        )

        if (timeRange != TimeRange.DAY && dailyGroups.isNotEmpty()) {
            DailyDetailSection(dailyGroups = dailyGroups)
        }

        if (timeRange != TimeRange.DAY) {
            ChartsSection(
                filteredTransactions = filteredTransactions,
                onTapTransaction = { tx ->
                    drillDownTitle = "${"%.2f".format(tx.amount)} - ${tx.description}"
                    drillDownTransactions = listOf(tx)
                }
            )
        }

        CategoryPieChartSection(
            categoryExpenses = categoryExpenses,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            onTapCategory = { cat ->
                drillDownTitle = cat
                drillDownTransactions = filteredTransactions.filter { it.category == cat }
            }
        )

        CategoryBreakdownSection(
            categoryExpenses = categoryExpenses,
            totalExpense = totalExpense,
            onTapCategory = { cat ->
                drillDownTitle = cat
                drillDownTransactions = filteredTransactions.filter { it.category == cat }
            }
        )

        if (filteredTransactions.isNotEmpty()) {
            AiMonthlySummarySection(
                viewModel = viewModel,
                timeRange = timeRange,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                customStart = customStart,
                customEnd = customEnd,
                filteredTransactions = filteredTransactions,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = netBalance,
                categoryExpenses = categoryExpenses
            )
        }
    }

    // 下钻详情弹窗
    if (showDrillDown) {
        DrillDownDialog(
            title = drillDownTitle,
            transactions = drillDownTransactions,
            onDismiss = { drillDownTransactions = emptyList() }
        )
    }
}

// ── 时间范围选择 ──
@Composable
private fun TimeRangeSelector(
    timeRange: TimeRange,
    onTimeRangeChange: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.entries.forEach { range ->
            TagChip(
                selected = timeRange == range,
                onClick = { onTimeRangeChange(range) },
                label = when (range) {
                    TimeRange.DAY -> "日"; TimeRange.WEEK -> "周"
                    TimeRange.MONTH -> "月"; TimeRange.YEAR -> "年"
                    TimeRange.CUSTOM -> "自定义"
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── 概要统计卡片 ──
@Composable
private fun AnalysisSummaryCard(
    timeRange: TimeRange,
    selectedYear: Int,
    selectedMonth: Int,
    customStart: Long?,
    customEnd: Long?,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    context: android.content.Context,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit
) {
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
                IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一个月",
                        tint = if (timeRange in listOf(TimeRange.DAY, TimeRange.MONTH, TimeRange.YEAR))
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else Color.Transparent, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = when (timeRange) {
                        TimeRange.DAY -> SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(System.currentTimeMillis()))
                        TimeRange.WEEK -> "最近7天"
                        TimeRange.MONTH -> "${selectedYear}年${selectedMonth + 1}月"
                        TimeRange.YEAR -> "${selectedYear}年"
                        TimeRange.CUSTOM -> {
                            val fmt = SimpleDateFormat("MM/dd", Locale.getDefault())
                            "${customStart?.let { fmt.format(Date(it)) } ?: "开始"} - ${customEnd?.let { fmt.format(Date(it)) } ?: "结束"}"
                        }
                    },
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(enabled = timeRange == TimeRange.CUSTOM) {
                        val now = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(y, m, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0)
                            onCustomRangeSelected(c.timeInMillis, c.apply { add(Calendar.DAY_OF_MONTH, 30) }.timeInMillis)
                        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下一个月",
                        tint = if (timeRange in listOf(TimeRange.MONTH, TimeRange.YEAR))
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else Color.Transparent, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem("收入", totalIncome, IncomeGreen)
                SummaryItem("支出", totalExpense, ExpenseRed)
                SummaryItem("结余", netBalance, if (netBalance >= 0) IncomeGreen else ExpenseRed)
            }

            if (totalIncome > 0 || totalExpense > 0) {
                Spacer(Modifier.height(12.dp))
                val ratio = if (totalIncome > 0) (totalExpense / totalIncome).toFloat().coerceIn(0f, 1f) else 1f
                val expensePct = (ratio * 100).toInt()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(DividerColor)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(ratio).background(ExpenseRed, RoundedCornerShape(4.dp)))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("支出${expensePct}% | 收入${100 - expensePct}%", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// ── 每日明细 ──
@Composable
private fun DailyDetailSection(dailyGroups: List<Map.Entry<Pair<Int, Int>, List<TransactionEntity>>>) {
    Spacer(Modifier.height(16.dp))
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
                    val dow = when (c.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "周一"; Calendar.TUESDAY -> "周二"
                        Calendar.WEDNESDAY -> "周三"; Calendar.THURSDAY -> "周四"
                        Calendar.FRIDAY -> "周五"; Calendar.SATURDAY -> "周六"
                        Calendar.SUNDAY -> "周日"; else -> ""
                    }
                    "${SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(firstTx.dateMillis))} $dow"
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
}

// ── 折线图 + 柱状图 ──
@Composable
private fun ChartsSection(
    filteredTransactions: List<TransactionEntity>,
    onTapTransaction: (TransactionEntity) -> Unit
) {
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "折线图") {
        LineChart(
            transactions = filteredTransactions,
            incomeColor = IncomeGreen,
            expenseColor = ExpenseRed,
            onTapTransaction = onTapTransaction
        )
    }

    Spacer(Modifier.height(16.dp))
    ChartCard(title = "柱状图") {
        BarChart(
            transactions = filteredTransactions,
            incomeColor = IncomeGreen,
            expenseColor = ExpenseRed,
            onTapTransaction = onTapTransaction
        )
    }
}

// ── 饼图 ──
@Composable
private fun CategoryPieChartSection(
    categoryExpenses: List<Map.Entry<String, Double>>,
    totalIncome: Double,
    totalExpense: Double,
    onTapCategory: (String) -> Unit
) {
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "饼图") {
        PieChart(
            categoryExpenses = categoryExpenses,
            catColors = remember {
                listOf(
                    Color(0xFFE8824A), Color(0xFF4CAF7A), Color(0xFF5B8DB8), Color(0xFFD4A06A),
                    Color(0xFFD4889A), Color(0xFF9B7BB5), Color(0xFF6BB5B5), Color(0xFFE8A06A),
                    Color(0xFFA0A0A0), Color(0xFF7AB87A)
                )
            },
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            onTapCategory = onTapCategory
        )
    }
}

// ── 支出分类进度条 ──
@Composable
private fun CategoryBreakdownSection(
    categoryExpenses: List<Map.Entry<String, Double>>,
    totalExpense: Double,
    onTapCategory: (String) -> Unit
) {
    if (categoryExpenses.isEmpty()) return

    val catColors = remember {
        listOf(
            Color(0xFFE8824A), Color(0xFF4CAF7A), Color(0xFF5B8DB8), Color(0xFFD4A06A),
            Color(0xFFD4889A), Color(0xFF9B7BB5), Color(0xFF6BB5B5), Color(0xFFE8A06A),
            Color(0xFFA0A0A0), Color(0xFF7AB87A)
        )
    }

    Spacer(Modifier.height(16.dp))
    val maxCatAmount = categoryExpenses.first().value.coerceAtLeast(1.0)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("支出分类", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            categoryExpenses.forEachIndexed { i, (cat, amount) ->
                val pct = amount / totalExpense.coerceAtLeast(1.0) * 100
                val barFraction = (amount / maxCatAmount).toFloat().coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable { onTapCategory(cat) },
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
@Composable
private fun AiMonthlySummarySection(
    viewModel: AnalysisViewModel,
    timeRange: TimeRange,
    selectedYear: Int,
    selectedMonth: Int,
    customStart: Long?,
    customEnd: Long?,
    filteredTransactions: List<TransactionEntity>,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    categoryExpenses: List<Map.Entry<String, Double>>
) {
    var summaryText by remember { mutableStateOf<String?>(null) }
    var summaryLoading by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(timeRange, selectedYear, selectedMonth, customStart, customEnd) {
        summaryText = null; summaryError = null
        if (filteredTransactions.isEmpty()) return@LaunchedEffect
        summaryLoading = true
        try {
            summaryText = viewModel.generateMonthlySummary(
                year = when (timeRange) {
                    TimeRange.DAY, TimeRange.WEEK, TimeRange.CUSTOM -> Calendar.getInstance().get(Calendar.YEAR)
                    TimeRange.MONTH -> selectedYear
                    TimeRange.YEAR -> selectedYear
                },
                month = when (timeRange) {
                    TimeRange.DAY -> Calendar.getInstance().get(Calendar.MONTH) + 1
                    TimeRange.MONTH -> selectedMonth + 1
                    else -> -1
                },
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = netBalance,
                expenseCategories = categoryExpenses.associate { it.key to it.value },
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
                Text("AI 月账单总结", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(10.dp))
            when {
                summaryLoading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("AI 正在分析你的账单...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                summaryError != null -> {
                    Text(summaryError!!, fontSize = 13.sp, color = ExpenseRed, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { summaryText = null; summaryError = null; summaryLoading = true }) {
                        Text("重试", fontSize = 13.sp)
                    }
                }
                summaryText != null -> {
                    Text(summaryText!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

// ── 下钻详情弹窗 ──
@Composable
private fun DrillDownDialog(
    title: String,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column {
                if (transactions.size == 1) {
                    val tx = transactions.first()
                    Text("类型：${if (tx.type == TransactionType.INCOME) "收入" else "支出"}", fontSize = 14.sp)
                    Text("金额：¥${"%.2f".format(tx.amount)}", fontSize = 14.sp)
                    if (tx.category.isNotBlank()) Text("分类：${tx.category}", fontSize = 14.sp)
                    if (tx.description.isNotBlank()) Text("备注：${tx.description}", fontSize = 14.sp)
                    Text("时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tx.dateMillis))}", fontSize = 14.sp)
                } else {
                    Text("共 ${transactions.size} 条记录", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                        transactions.sortedByDescending { it.dateMillis }.forEach { tx ->
                            AnalysisTransactionRow(tx = tx)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun AnalysisTransactionRow(tx: TransactionEntity) {
    TransactionCard(transaction = tx, compactMode = true)
}
