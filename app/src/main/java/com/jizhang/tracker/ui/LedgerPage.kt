package com.jizhang.tracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhang.tracker.R
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionType
import com.jizhang.tracker.ui.components.PrivacyFooter
import com.jizhang.tracker.ui.components.TransactionList
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerPage(
    todayTransactions: List<TransactionEntity>,
    recentTransactions: List<TransactionEntity>,
    todayIncome: Double,
    todayExpense: Double,
    allTransactions: List<TransactionEntity>,
    isManualMode: Boolean,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    installDateMillis: Long,
    onRequestSmsPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onToggleMode: () -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit,
    onDateFilterClick: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onDetailClick: (TransactionEntity) -> Unit,
    snackbarHostState: SnackbarHostState,
    updateVersionName: String? = null,
    onUpdateNow: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val isAllPage = pagerState.currentPage == 1

    val totalIncome = remember(allTransactions, installDateMillis) {
        allTransactions.filter { it.type == TransactionType.INCOME && it.dateMillis >= installDateMillis }
            .sumOf { it.amount }
    }
    val totalExpense = remember(allTransactions, installDateMillis) {
        allTransactions.filter { it.type == TransactionType.EXPENSE && it.dateMillis >= installDateMillis }
            .sumOf { it.amount }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // SMS 权限提示
        if (!hasSmsPermission && !isManualMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "需要短信权限读取微信/支付宝账单",
                        fontSize = 13.sp,
                        color = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRequestSmsPermission) {
                        Text("授予权限", fontSize = 13.sp, color = WarningOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 通知权限提示
        if (!isManualMode && hasSmsPermission && !hasNotificationPermission) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "开启通知监听可自动读取微信/支付宝支付通知",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text("去开启", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 更新提示横幅
        if (!updateVersionName.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ledger_update_banner, updateVersionName),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onUpdateNow) {
                        Text(stringResource(R.string.ledger_update_now), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onDismissUpdate) {
                        Text(stringResource(R.string.ledger_update_dismiss), fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // 刷新加载指示器
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }

        // 余额卡片
        WeChatStyleBalanceCard(
            income = if (isAllPage) totalIncome else todayIncome,
            expense = if (isAllPage) totalExpense else todayExpense,
            title = if (isAllPage) "总净收入" else "今日净收入",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            isManualMode = isManualMode,
            onToggleMode = onToggleMode,
            onDateFilterClick = onDateFilterClick,
            onRefresh = onRefresh,
            onSearchClick = onSearchClick
        )

        // Tab 切换
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                shape = RoundedCornerShape(
                    topStart = 12.dp, bottomStart = 12.dp,
                    topEnd = 0.dp, bottomEnd = 0.dp
                ),
                color = if (pagerState.currentPage == 0) selectedColor else CardBg(),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "今日明细",
                    fontWeight = if (pagerState.currentPage == 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (pagerState.currentPage == 0) Color.White else unselectedColor,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
            Surface(
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                shape = RoundedCornerShape(
                    topStart = 0.dp, bottomStart = 0.dp,
                    topEnd = 12.dp, bottomEnd = 12.dp
                ),
                color = if (pagerState.currentPage == 1) selectedColor else CardBg(),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "全部记录",
                    fontWeight = if (pagerState.currentPage == 1) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (pagerState.currentPage == 1) Color.White else unselectedColor,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }

        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> TransactionList(
                    transactions = todayTransactions,
                    emptyText = "未收取到数据",
                    onDelete = { id ->
                        onDeleteTransaction(id)
                        scope.launch { snackbarHostState.showSnackbar("已删除") }
                    },
                    onItemClick = onDetailClick
                )
                1 -> TransactionList(
                    transactions = recentTransactions,
                    emptyText = "暂无记录",
                    onDelete = { id ->
                        onDeleteTransaction(id)
                        scope.launch { snackbarHostState.showSnackbar("已删除") }
                    },
                    onItemClick = onDetailClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        PrivacyFooter()
    }
}
