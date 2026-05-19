package com.example.billtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.ui.AlipayBlue
import com.example.billtracker.ui.CardBg
import com.example.billtracker.ui.ExpenseRed
import com.example.billtracker.ui.IncomeGreen
import com.example.billtracker.ui.WechatGreen
import java.text.SimpleDateFormat
import java.util.*

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
        val a11yDetailDesc = "交易详情：$sourceLabel $typeLabel $typePrefix¥${"%.2f".format(transaction.amount)}"
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.55f)
                .semantics { contentDescription = a11yDetailDesc },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = dateStr, fontSize = 11.sp, color = Color(0xFF9AA0A6))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$typePrefix¥%.2f".format(transaction.amount),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = originalSms,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF1F3F4))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "备注",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = { Text("添加备注...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .semantics { contentDescription = "备注编辑框" },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFDADCE0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
