package com.example.billtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionType
import com.example.billtracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val periods = remember { viewModel.getAnalysisPeriods() }
    var selectedPeriodIndex by remember { mutableIntStateOf(0) }
    val transactions = remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // 加载数据
    LaunchedEffect(selectedPeriodIndex) {
        val start = periods.getOrNull(selectedPeriodIndex)?.startMillis ?: 0L
        transactions.value = viewModel.getAnalysisTransactions(start)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账单分析", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回", color = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 时段选择器 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEachIndexed { i, period ->
                    FilterChip(
                        selected = i == selectedPeriodIndex,
                        onClick = { selectedPeriodIndex = i },
                        label = { Text(period.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            val txList = transactions.value
            val income = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val net = income - expense

            // ── 概要统计 ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("收入", "¥%.2f".format(income), IncomeGreen)
                    StatItem("支出", "¥%.2f".format(expense), ExpenseRed)
                    StatItem("净收入", "¥%.2f".format(net), if (net >= 0) IncomeGreen else ExpenseRed)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 柱状图（每日收支对比） ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("收支对比", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    if (txList.isEmpty()) {
                        Text("暂无数据", color = Color(0xFF9AA0A6), modifier = Modifier.padding(vertical = 40.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    } else {
                        BarChart(txList, selectedPeriodIndex)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 饼图（分类占比） ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("支出分类", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    if (txList.none { it.type == TransactionType.EXPENSE }) {
                        Text("暂无支出数据", color = Color(0xFF9AA0A6), modifier = Modifier.padding(vertical = 40.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    } else {
                        PieChart(txList.filter { it.type == TransactionType.EXPENSE })
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color(0xFF9AA0A6))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private data class DaySummary(val label: String, val income: Double, val expense: Double, val incomeColor: Color, val expenseColor: Color)

@Composable
private fun BarChart(txList: List<TransactionEntity>, periodIndex: Int) {
    // 按天分组
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    val dayGroups = txList.groupBy {
        cal.timeInMillis = it.dateMillis
        cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
    }.entries.map { (_, txs) ->
        val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        cal.timeInMillis = txs.first().dateMillis
        DaySummary(sdf.format(cal.time), dayIncome, dayExpense, IncomeGreen, ExpenseRed)
    }.sortedBy { it.label }

    val maxVal = (dayGroups.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0).coerceAtLeast(1.0)

    val barWidth = 28f
    val gap = 12f
    val totalWidth = dayGroups.size * (barWidth * 2 + gap) + 32f

    Column {
        // 图例
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("收入", IncomeGreen)
            LegendItem("支出", ExpenseRed)
        }
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Canvas(
                modifier = Modifier.width((dayGroups.size * (barWidth * 2 + gap) + 32).dp).height(180.dp)
            ) {
                val chartLeft = 16f
                val chartTop = 8f
                val chartHeight = size.height - 40f
                val chartWidth = size.width - 32f

                dayGroups.forEachIndexed { i, day ->
                    val x = chartLeft + i * (barWidth * 2 + gap)
                    val incomeH = (day.income / maxVal * chartHeight).toFloat().coerceAtLeast(0f)
                    val expenseH = (day.expense / maxVal * chartHeight).toFloat().coerceAtLeast(0f)

                    // 收入柱（绿色）
                    drawRect(
                        color = day.incomeColor,
                        topLeft = Offset(x, chartTop + chartHeight - incomeH),
                        size = Size(barWidth, incomeH)
                    )
                    // 支出柱（红色）
                    drawRect(
                        color = day.expenseColor,
                        topLeft = Offset(x + barWidth + 4f, chartTop + chartHeight - expenseH),
                        size = Size(barWidth, expenseH)
                    )
                }

                // 底部日期标签
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9AA0A6")
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                dayGroups.forEachIndexed { i, day ->
                    val cx = chartLeft + i * (barWidth * 2 + gap) + barWidth + 2f
                    drawContext.canvas.nativeCanvas.drawText(day.label, cx, size.height - 4f, paint)
                }
            }
        }
    }
}

@Composable
private fun PieChart(expenses: List<TransactionEntity>) {
    val catMap = expenses.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val total = catMap.values.sum().coerceAtLeast(1.0)
    val sorted = catMap.entries.sortedByDescending { it.value }

    val colors = listOf(
        Color(0xFFE8824A), Color(0xFF4CAF7A), Color(0xFF5B8DB8), Color(0xFFD4A06A),
        Color(0xFFD4889A), Color(0xFF9B7BB5), Color(0xFF6BB5B5), Color(0xFFE8A06A),
        Color(0xFFA0A0A0), Color(0xFF7AB87A)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 饼图
        Canvas(
            modifier = Modifier.size(160.dp)
        ) {
            val strokeWidth = 48f
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            var startAngle = -90f

            sorted.forEachIndexed { i, (_, amount) ->
                val sweep = (amount.toFloat() / total.toFloat() * 360f).coerceAtLeast(0.5f)
                drawArc(
                    color = colors[i % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweep
            }
        }

        Spacer(Modifier.width(16.dp))

        // 图例
        Column(modifier = Modifier.weight(1f)) {
            sorted.forEachIndexed { i, (cat, amount) ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[i % colors.size], RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(cat, fontSize = 13.sp, color = Color(0xFF5F6368), modifier = Modifier.weight(1f))
                    Text("%.1f%%".format(amount / total * 100), fontSize = 12.sp, color = Color(0xFF9AA0A6))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF9AA0A6))
    }
}
