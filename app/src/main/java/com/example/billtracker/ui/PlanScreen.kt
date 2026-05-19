package com.example.billtracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.ui.components.TagChip
import com.example.billtracker.data.CustomPlan
import com.example.billtracker.data.PlanDataType
import com.example.billtracker.data.displayName
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlanScreen(
    todayIncome: Double,
    todayExpense: Double,
    totalIncome: Double,
    totalExpense: Double,
    planBalance: Double,
    todayPlanTarget: Double,
    totalPlanTarget: Double,
    savePlanTarget: Double,
    todayPlanNote: String,
    totalPlanNote: String,
    savePlanNote: String,
    customPlans: List<CustomPlan>,
    onBalanceChange: (Double) -> Unit,
    onTodayPlanTargetSave: (Double) -> Unit,
    onTotalPlanTargetSave: (Double) -> Unit,
    onSavePlanTargetSave: (Double) -> Unit,
    onTodayPlanNoteSave: (String) -> Unit,
    onTotalPlanNoteSave: (String) -> Unit,
    onSavePlanNoteSave: (String) -> Unit,
    onUpdateCustomPlan: (Int, Double, String) -> Unit,
    onDeleteCustomPlan: (Int) -> Unit = {}
) {
    val effectiveBalance = if (planBalance > 0) planBalance
        else (totalIncome - totalExpense).coerceAtLeast(0.0)

    var showTodayPlanDetail by remember { mutableStateOf(false) }
    var showTotalPlanDetail by remember { mutableStateOf(false) }
    var showSavePlanDetail by remember { mutableStateOf(false) }
    var customPlanDetailIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── 余额（微信风格卡片） ──
        BalanceCard(
            balance = effectiveBalance,
            todayIncome = todayIncome,
            totalIncome = totalIncome,
            onBalanceChange = onBalanceChange
        )

        Spacer(Modifier.height(16.dp))

        // ── 今日计划 ──
        PlanCard(
            title = "今日计划",
            target = todayPlanTarget,
            currentAmount = effectiveBalance,
            note = todayPlanNote,
            onClick = { showTodayPlanDetail = true }
        )

        Spacer(Modifier.height(12.dp))

        // ── 省钱计划 ──
        SavePlanCard(
            target = savePlanTarget,
            todayExpense = todayExpense,
            note = savePlanNote,
            onClick = { showSavePlanDetail = true }
        )

        Spacer(Modifier.height(12.dp))

        // ── 总计划 ──
        PlanCard(
            title = "总计划",
            target = totalPlanTarget,
            currentAmount = effectiveBalance,
            note = totalPlanNote,
            onClick = { showTotalPlanDetail = true }
        )

        // ── 自定义计划（支持左滑删除） ──
        customPlans.forEachIndexed { index, plan ->
            val planCurrent = when (plan.type) {
                PlanDataType.TODAY_INCOME -> todayIncome
                PlanDataType.TODAY_EXPENSE -> todayExpense
                PlanDataType.TODAY_NET -> todayIncome - todayExpense
                PlanDataType.TOTAL_INCOME -> totalIncome
                PlanDataType.TOTAL_EXPENSE -> totalExpense
                PlanDataType.TOTAL_NET -> totalIncome - totalExpense
            }
            Spacer(Modifier.height(12.dp))
            SwipeablePlanCard(
                title = plan.name,
                target = plan.target,
                currentAmount = planCurrent,
                note = plan.note,
                onClick = { customPlanDetailIndex = index },
                onDelete = { onDeleteCustomPlan(index) }
            )
        }

        Spacer(Modifier.height(16.dp))
        PrivacyFooter()
    }

    if (showTodayPlanDetail) {
        PlanDetailDialog(
            title = "今日计划",
            target = todayPlanTarget,
            currentAmount = effectiveBalance,
            note = todayPlanNote,
            onDismiss = { showTodayPlanDetail = false },
            onSave = { target, note ->
                onTodayPlanTargetSave(target)
                onTodayPlanNoteSave(note)
                showTodayPlanDetail = false
            }
        )
    }
    if (showSavePlanDetail) {
        SavePlanDetailDialog(
            target = savePlanTarget,
            todayExpense = todayExpense,
            note = savePlanNote,
            onDismiss = { showSavePlanDetail = false },
            onSave = { target, note ->
                onSavePlanTargetSave(target)
                onSavePlanNoteSave(note)
                showSavePlanDetail = false
            }
        )
    }
    if (showTotalPlanDetail) {
        PlanDetailDialog(
            title = "总计划",
            target = totalPlanTarget,
            currentAmount = effectiveBalance,
            note = totalPlanNote,
            onDismiss = { showTotalPlanDetail = false },
            onSave = { target, note ->
                onTotalPlanTargetSave(target)
                onTotalPlanNoteSave(note)
                showTotalPlanDetail = false
            }
        )
    }
    if (customPlanDetailIndex >= 0 && customPlanDetailIndex < customPlans.size) {
        val plan = customPlans[customPlanDetailIndex]
        val planCurrent = when (plan.type) {
            PlanDataType.TODAY_INCOME -> todayIncome
            PlanDataType.TODAY_EXPENSE -> todayExpense
            PlanDataType.TODAY_NET -> todayIncome - todayExpense
            PlanDataType.TOTAL_INCOME -> totalIncome
            PlanDataType.TOTAL_EXPENSE -> totalExpense
            PlanDataType.TOTAL_NET -> totalIncome - totalExpense
        }
        PlanDetailDialog(
            title = plan.name,
            target = plan.target,
            currentAmount = planCurrent,
            note = plan.note,
            onDismiss = { customPlanDetailIndex = -1 },
            onSave = { target, note ->
                onUpdateCustomPlan(customPlanDetailIndex, target, note)
                customPlanDetailIndex = -1
            }
        )
    }
}

// ── 初始余额卡片（仿微信风格） ──
@Composable
private fun BalanceCard(
    balance: Double,
    todayIncome: Double,
    totalIncome: Double,
    onBalanceChange: (Double) -> Unit
) {
    var balanceText by remember(balance) {
        mutableStateOf(if (balance == 0.0) "" else "%.2f".format(balance))
    }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("余额", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        if (isEditing) {
                            balanceText.toDoubleOrNull()?.let { onBalanceChange(it) }
                        }
                        isEditing = !isEditing
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (isEditing) "完成" else "编辑",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("0") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
            } else if (balance > 0) {
                Text(
                    text = "¥%.2f".format(balance),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable { isEditing = true }
                )
            } else {
                Text(
                    text = "点击设置余额",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable {
                        balanceText = ""
                        isEditing = true
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("今日收入", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text(
                        "¥%.2f".format(todayIncome),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总收入", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Text(
                        "¥%.2f".format(totalIncome),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

// ── 计划卡片 ──
@Composable
fun PlanCard(
    title: String,
    target: Double,
    currentAmount: Double,
    note: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))

            if (target <= 0) {
                // 无计划
                Text(
                    text = "无计划",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "点击设置目标金额",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                // 目标金额
                Text(
                    text = "目标 ¥%.2f".format(target),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(6.dp))

                // 进度信息
                val progress = (currentAmount / target).coerceIn(0.0, 1.0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "当前 ¥%.2f".format(currentAmount),
                        fontSize = 13.sp,
                        color = if (currentAmount >= target) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "%d%%".format((progress * 100).toInt()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (progress >= 1f) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // 进度条
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.toFloat())
                            .fillMaxHeight()
                            .background(
                                if (progress >= 1f) IncomeGreen else MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            if (note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── 可左滑删除的计划卡片（用于自定义计划） ──
@Composable
private fun SwipeablePlanCard(
    title: String,
    target: Double,
    currentAmount: Double,
    note: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val buttonAreaPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(modifier = Modifier.clipToBounds().fillMaxWidth()) {
        // 背景：删除按钮
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    scope.launch { offsetX.animateTo(0f) }
                    onDelete()
                },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEA4335)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }

        // 前景：可滑动的卡片
        Box(modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
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
                            offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-buttonAreaPx, 0f))
                        }
                    }
                )
            }
        ) {
            PlanCard(
                title = title,
                target = target,
                currentAmount = currentAmount,
                note = note,
                onClick = onClick
            )
        }
    }
}

// ── 计划详情弹窗 ──
@Composable
fun PlanDetailDialog(
    title: String,
    target: Double,
    currentAmount: Double,
    note: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var targetText by remember(target) {
        mutableStateOf(if (target <= 0) "" else "%.2f".format(target))
    }
    var noteInput by remember { mutableStateOf(note) }

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
            colors = CardDefaults.cardColors(containerColor = CardBg())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── 标题 ──
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // ── 目标金额输入 ──
                Text("目标金额", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("0") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(12.dp))

                // ── 当前进度 ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (currentAmount > 0) "当前进度: ¥%.2f".format(currentAmount) else "您还没有设置计划金额",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = if (currentAmount > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── 分隔线 ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(12.dp))

                // ── 备注 ──
                Text(
                    text = "备注",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("添加备注...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.height(12.dp))

                // ── 取消 / 保存 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val t = targetText.toDoubleOrNull() ?: 0.0
                            onSave(t, noteInput)
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

// ── 省钱计划卡片 ──
@Composable
fun SavePlanCard(
    target: Double,
    todayExpense: Double,
    note: String,
    onClick: () -> Unit
) {
    val remaining = target - todayExpense
    val isOverBudget = remaining < 0 && target > 0
    val progress = if (target > 0) (todayExpense / target).coerceIn(0.0, 1.0) else 0.0

    val barColor = when {
        progress >= 1f -> ExpenseRed
        progress >= 0.7f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("省钱计划", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))

            if (target <= 0) {
                Text(
                    text = "无计划",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "点击设置目标金额",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                if (isOverBudget) {
                    Text(
                        text = "已超出 ¥%.2f".format(-remaining),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed,
                        letterSpacing = 0.5.sp
                    )
                } else {
                    Text(
                        text = "还可以花 ¥%.2f".format(remaining),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "今日支出 ¥%.2f".format(todayExpense),
                        fontSize = 13.sp,
                        color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "%d%%".format((progress * 100).toInt()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (progress >= 1f) ExpenseRed else if (progress >= 0.7f) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.toFloat())
                            .fillMaxHeight()
                            .background(barColor, RoundedCornerShape(3.dp))
                    )
                }
            }

            if (note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = note,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── 省钱计划详情弹窗 ──
@Composable
fun SavePlanDetailDialog(
    target: Double,
    todayExpense: Double,
    note: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var targetText by remember(target) {
        mutableStateOf(if (target <= 0) "" else "%.2f".format(target))
    }
    var noteInput by remember { mutableStateOf(note) }

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
            colors = CardDefaults.cardColors(containerColor = CardBg())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "省钱计划",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Text("目标金额", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text("0") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "今日支出: ¥%.2f".format(todayExpense),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "备注",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("添加备注...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val t = targetText.toDoubleOrNull() ?: 0.0
                            onSave(t, noteInput)
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

// ── 添加计划弹窗（自定义计划名称 + 类型选择） ──
@Composable
fun AddPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, note: String, type: PlanDataType) -> Unit
) {
    var planName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(PlanDataType.TODAY_NET) }

    val allTypes = PlanDataType.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("添加计划", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 计划名称（可编辑）
                Text("计划名称", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it; nameError = false },
                    placeholder = { Text("输入计划名称...") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("请输入计划名称", color = Color(0xFFEA4335)) }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 选择类型
                Text("选择类型", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // 第一行：今日收入/今日支出/今日净收入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTypes.take(3).forEach { type ->
                        TagChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = type.displayName(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // 第二行：总支出/总收入/总净收入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTypes.drop(3).forEach { type ->
                        TagChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = type.displayName(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 设置金额
                Text("设置金额", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; amountError = false },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError,
                    supportingText = if (amountError) {{ Text("请输入有效金额", color = Color(0xFFEA4335)) }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 备注
                Text("备注", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("添加备注...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = planName.isBlank()
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                    }
                    if (!nameError && !amountError) {
                        onConfirm(planName.trim(), amount!!, note, selectedType)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("添加", modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

