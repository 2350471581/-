package com.jizhang.tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class WeChatStyleBalanceCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun positiveBalance_showsNetIncome() {
        composeTestRule.setContent {
            BillTrackerTheme {
                WeChatStyleBalanceCard(
                    income = 100.0,
                    expense = 30.0,
                    title = "今日净收入"
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
            "今日净收入：净收入 ¥70.00，收入 ¥100.00，支出 ¥30.00"
        ).assertIsDisplayed()
    }

    @Test
    fun negativeBalance_showsNetExpense() {
        composeTestRule.setContent {
            BillTrackerTheme {
                WeChatStyleBalanceCard(
                    income = 20.0,
                    expense = 50.0,
                    title = "今日净收入"
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
            "今日净收入：净支出 ¥30.00，收入 ¥20.00，支出 ¥50.00"
        ).assertIsDisplayed()
    }

    @Test
    fun zeroBalance_showsZeroNet() {
        composeTestRule.setContent {
            BillTrackerTheme {
                WeChatStyleBalanceCard(
                    income = 0.0,
                    expense = 0.0,
                    title = "今日净收入"
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
            "今日净收入：净收入 ¥0.00，收入 ¥0.00，支出 ¥0.00"
        ).assertIsDisplayed()
    }
}
