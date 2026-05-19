package com.example.billtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val incomeLabel = "收入 ¥${"%.2f".format(income)}"
    val expenseLabel = "支出 ¥${"%.2f".format(expense)}"
    val netLabel = if (net >= 0) "净收入 ¥${"%.2f".format(net)}" else "净支出 ¥${"%.2f".format(-net)}"
    val a11yBalanceDesc = "$title：$netLabel，$incomeLabel，$expenseLabel"

    Card(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = a11yBalanceDesc
        },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

            Text(
                text = if (net >= 0) "+¥%.2f".format(net) else "-¥%.2f".format(-net),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0) IncomeGreen else ExpenseRed,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(14.dp))

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
