package com.jizhang.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionSource
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.data.TransactionCategory
import com.jizhang.tracker.ui.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 统一的交易条目卡片，支持紧凑模式（不显示分类、备注标记）和详情模式。
 */
@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val dateStr = remember(transaction.dateMillis) {
        val sdf = SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.dateMillis))
    }
    val displayDesc = transaction.description
        .replace(Regex("""【[^】]*】"""), "")
        .trim()
        .take(if (compactMode) 20 else 30)

    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
    }
    val typePrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
    }

    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> stringResource(R.string.tx_card_income)
        TransactionType.EXPENSE -> stringResource(R.string.tx_card_expense)
    }
    val a11yDescription = "${typeLabel}，¥${"%.2f".format(transaction.amount)}，${transaction.category}，${sourceDisplayName(transaction.source)}"

    Card(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = a11yDescription
        },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactMode) 56.dp else 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧色条
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (compactMode) 56.dp else 64.dp)
                    .background(amountColor, RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp))
            )
            Spacer(Modifier.width(14.dp))

            // 来源标签
            SourceLabel(source = transaction.source)

            // 非紧凑模式显示分类
            if (!compactMode && transaction.category != "其他") {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${TransactionCategory.getIcon(transaction.category)} ${transaction.category}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // 描述 + 时间
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayDesc.ifEmpty { defaultDescription(transaction.source) },
                    fontSize = if (compactMode) 13.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!compactMode) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = dateStr, fontSize = 11.sp, color = SubtleText)
                        if (transaction.description.contains("\n--备注--\n")) {
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.tx_card_note_mark), fontSize = 9.sp, color = MutedIconColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 金额
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

private fun defaultDescription(source: TransactionSource): String = when (source) {
    TransactionSource.MANUAL -> "手动记账"
    TransactionSource.BANK -> "银行账单"
    else -> "账单"
}
