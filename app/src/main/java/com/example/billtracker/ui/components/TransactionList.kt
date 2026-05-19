package com.example.billtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billtracker.data.TransactionEntity

@Composable
fun TransactionList(
    transactions: List<TransactionEntity>,
    emptyText: String,
    onDelete: (Long) -> Unit,
    onItemClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val prevCount = remember { mutableIntStateOf(transactions.size) }

    LaunchedEffect(transactions.size) {
        if (transactions.size > prevCount.intValue && transactions.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
        prevCount.intValue = transactions.size
    }

    if (transactions.isEmpty()) {
        val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        val hint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        Box(
            modifier = Modifier.fillMaxSize().then(modifier),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = emptyText,
                    color = muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击右下角 + 手动记账",
                    color = hint,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().then(modifier),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDelete = { onDelete(transaction.id) },
                    onItemClick = { onItemClick(transaction) }
                )
            }
        }
    }
}
