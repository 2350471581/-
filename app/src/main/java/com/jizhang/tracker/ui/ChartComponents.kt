package com.jizhang.tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionType
import java.text.SimpleDateFormat
import java.util.*

data class ChartLabel(val label: String, val amount: Double, val isIncome: Boolean)

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clearAndSetSemantics { contentDescription = "图表：$title" },
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
fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(4.dp))
        Text("¥%.2f".format(amount), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

// ── 折线图 ──
@Composable
fun LineChart(
    transactions: List<TransactionEntity>,
    incomeColor: Color,
    expenseColor: Color,
    onTapTransaction: ((TransactionEntity) -> Unit)? = null
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
                            if (selectedIndex == tapIdx) {
                                onTapTransaction?.invoke(sorted.getOrNull(tapIdx) ?: return@detectTapGestures)
                            } else {
                                selectedIndex = tapIdx
                            }
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

// ── 柱状图 ──
@Composable
fun BarChart(
    transactions: List<TransactionEntity>,
    incomeColor: Color,
    expenseColor: Color,
    onTapTransaction: ((TransactionEntity) -> Unit)? = null
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
                            if (selectedIndex == tapIdx) {
                                // 再次点击已选中的柱子，下钻查看详情
                                onTapTransaction?.invoke(sorted.getOrNull(tapIdx) ?: return@detectTapGestures)
                            } else {
                                selectedIndex = tapIdx
                            }
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

// ── 饼图 ──
@Composable
fun PieChart(
    categoryExpenses: List<Map.Entry<String, Double>>,
    catColors: List<Color>,
    totalIncome: Double,
    totalExpense: Double,
    onTapCategory: ((String) -> Unit)? = null
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
                        color = DividerColor,
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
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp)
                            .clickable(enabled = onTapCategory != null) { onTapCategory?.invoke(cat) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
