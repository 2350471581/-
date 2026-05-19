package com.example.billtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    allTransactions: List<TransactionEntity>
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus search field
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    // 300ms debounce
    LaunchedEffect(query) {
        delay(300)
        debouncedQuery = query
    }

    // Filter transactions
    val filteredTransactions = remember(debouncedQuery, allTransactions) {
        if (debouncedQuery.isBlank()) {
            allTransactions
        } else {
            val q = debouncedQuery.lowercase()
            allTransactions.filter { tx ->
                tx.description.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.amount.toString().contains(q)
            }
        }
    }

    // System back
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索账单", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search text field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("搜索描述、分类、金额...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFDADCE0)
                )
            )

            // Filter hint
            if (debouncedQuery.isNotBlank() && filteredTransactions.isNotEmpty()) {
                Text(
                    text = "找到 ${filteredTransactions.size} 条记录",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // Results
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "未找到匹配的记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "试试其他关键词",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        SearchResultCard(transaction = transaction)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(transaction: TransactionEntity) {
    val dateStr = remember(transaction.dateMillis) {
        val sdf = SimpleDateFormat("yy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.dateMillis))
    }
    val displayDesc = transaction.description
        .replace(Regex("""【[^】]*】"""), "")
        .trim()
        .take(30)

    val sourceLabel = when (transaction.source) {
        TransactionSource.WECHAT -> "微信"
        TransactionSource.ALIPAY -> "支付宝"
        TransactionSource.MANUAL -> "其他"
        TransactionSource.BANK -> "银行"
    }
    val sourceColor = when (transaction.source) {
        TransactionSource.WECHAT -> WechatGreen
        TransactionSource.ALIPAY -> AlipayBlue
        TransactionSource.MANUAL -> SubtleText
        TransactionSource.BANK -> Color(0xFFE65100)
    }
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
    }
    val typePrefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
    }
    val typeLabel = when (transaction.type) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color bar (income green / expense red)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(56.dp)
                    .background(
                        color = amountColor,
                        shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main content
            Column(modifier = Modifier.weight(1f)) {
                // Description row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayDesc.ifEmpty {
                            when (transaction.source) {
                                TransactionSource.MANUAL -> "手动记账"
                                TransactionSource.BANK -> "银行账单"
                                else -> "账单"
                            }
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = amountColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = typeLabel,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = amountColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Date + category + source row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = dateStr, fontSize = 11.sp, color = SubtleText)

                    if (transaction.category != "其他") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = transaction.category,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = sourceColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = sourceLabel,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = sourceColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            Text(
                text = "$typePrefix¥%.2f".format(transaction.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
