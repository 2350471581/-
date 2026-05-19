package com.jizhang.tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.ui.components.TransactionCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    allTransactions: List<TransactionEntity>,
    searchHistory: List<String> = emptyList(),
    onSearchHistoryChange: (List<String>) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val fieldSemantics = stringResource(R.string.search_field_semantics)

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        delay(300)
        debouncedQuery = query
    }

    // Save completed searches to history (not partial typing)
    LaunchedEffect(debouncedQuery) {
        if (debouncedQuery.isNotBlank() && debouncedQuery !in searchHistory) {
            onSearchHistoryChange(listOf(debouncedQuery) + searchHistory.take(19))
        }
    }

    val filteredTransactions = remember(debouncedQuery, allTransactions) {
        if (debouncedQuery.isBlank()) emptyList()
        else {
            val q = debouncedQuery.lowercase()
            allTransactions.filter { tx ->
                tx.description.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.amount.toString().contains(q)
            }
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.search_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = fieldSemantics },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = BorderColor)
            )

            // Search history (shown only when no active search)
            if (debouncedQuery.isBlank() && searchHistory.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.search_history_title), fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        TextButton(onClick = { showClearHistoryConfirm = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text(stringResource(R.string.search_history_clear), fontSize = 12.sp, color = ExpenseRed)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(searchHistory, key = { it }) { item ->
                            Surface(
                                onClick = { query = item },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onSearchHistoryChange(searchHistory - item) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_history_delete_cd),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Result count
            if (debouncedQuery.isNotBlank() && filteredTransactions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_result_count, filteredTransactions.size),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // Results or empty state
            if (debouncedQuery.isNotBlank()) {
                if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.search_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 15.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.search_try_other),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { transaction ->
                            SearchResultCard(transaction = transaction)
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            // Empty state when no query and no history
            if (debouncedQuery.isBlank() && searchHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.search_history_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 15.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // ── Clear history confirm ──
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.search_history_clear_confirm), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            confirmButton = {
                Button(onClick = { onSearchHistoryChange(emptyList()); showClearHistoryConfirm = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)) {
                    Text(stringResource(R.string.search_history_clear), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text(stringResource(R.string.profile_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun SearchResultCard(transaction: TransactionEntity) {
    TransactionCard(transaction = transaction, compactMode = false)
}
