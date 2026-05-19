package com.jizhang.tracker.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.jizhang.tracker.ui.BillTrackerTheme
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionSource
import com.jizhang.tracker.data.TransactionType
import org.junit.Rule
import org.junit.Test

class LedgerPageTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val todayTx = listOf(
        TransactionEntity(
            id = 1, amount = 35.0, type = TransactionType.EXPENSE,
            source = TransactionSource.MANUAL, description = "午餐",
            dateMillis = System.currentTimeMillis(), category = "餐饮"
        )
    )

    @Test
    fun displaysIncomeAndExpense() {
        composeTestRule.setContent {
            BillTrackerTheme {
                LedgerPage(
                    todayTransactions = todayTx,
                    recentTransactions = emptyList(),
                    todayIncome = 100.0,
                    todayExpense = 35.0,
                    allTransactions = todayTx,
                    isManualMode = true,
                    hasSmsPermission = false,
                    hasNotificationPermission = false,
                    installDateMillis = System.currentTimeMillis(),
                    onRequestSmsPermission = {},
                    onOpenNotificationSettings = {},
                    onToggleMode = {},
                    onRefresh = {},
                    onSearchClick = {},
                    onDateFilterClick = {},
                    onDeleteTransaction = {},
                    onDetailClick = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }
        // Balance card should show today's data
        val a11yDesc = "今日净收入：净收入 ¥65.00，收入 ¥100.00，支出 ¥35.00"
        composeTestRule.onNodeWithContentDescription(a11yDesc).assertIsDisplayed()
    }

    @Test
    fun todayDetailTab_isDisplayed() {
        composeTestRule.setContent {
            BillTrackerTheme {
                LedgerPage(
                    todayTransactions = todayTx,
                    recentTransactions = emptyList(),
                    todayIncome = 0.0,
                    todayExpense = 0.0,
                    allTransactions = emptyList(),
                    isManualMode = true,
                    hasSmsPermission = false,
                    hasNotificationPermission = false,
                    installDateMillis = System.currentTimeMillis(),
                    onRequestSmsPermission = {},
                    onOpenNotificationSettings = {},
                    onToggleMode = {},
                    onRefresh = {},
                    onSearchClick = {},
                    onDateFilterClick = {},
                    onDeleteTransaction = {},
                    onDetailClick = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }
        composeTestRule.onNodeWithText("今日明细").assertIsDisplayed()
        composeTestRule.onNodeWithText("全部记录").assertIsDisplayed()
    }
}
