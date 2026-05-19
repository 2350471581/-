package com.jizhang.tracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.jizhang.tracker.ui.BillTrackerTheme
import com.jizhang.tracker.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddTransactionDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun dialogTitle_isDisplayed() {
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithText("添加账单").assertIsDisplayed()
    }

    @Test
    fun typeChips_areDisplayed() {
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithContentDescription("支出类型").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("收入类型").assertIsDisplayed()
    }

    @Test
    fun confirmWithValidAmount_triggersCallback() {
        var confirmedAmount = -1.0
        var confirmedType = TransactionType.EXPENSE
        var confirmedNote = ""
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(
                    onDismiss = {},
                    onConfirm = { amount, type, note ->
                        confirmedAmount = amount
                        confirmedType = type
                        confirmedNote = note
                    }
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("金额输入框").performTextInput("35.00")
        composeTestRule.onNodeWithContentDescription("备注输入框").performTextInput("午餐")
        composeTestRule.onNodeWithContentDescription("添加账单").performClick()

        assertEquals(35.0, confirmedAmount, 0.001)
        assertEquals(TransactionType.EXPENSE, confirmedType)
        assertEquals("午餐", confirmedNote)
    }

    @Test
    fun confirmWithEmptyAmount_showsError() {
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithContentDescription("添加账单").performClick()
        composeTestRule.onNodeWithText("请输入有效金额").assertIsDisplayed()
    }

    @Test
    fun incomeTypeSelection_switchesType() {
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithContentDescription("收入类型").performClick()
        composeTestRule.onNodeWithContentDescription("金额输入框").performTextInput("100")
        composeTestRule.onNodeWithContentDescription("添加账单").performClick()
    }

    @Test
    fun cancel_dismissesDialog() {
        var dismissed = false
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(onDismiss = { dismissed = true }, onConfirm = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithText("取消").performClick()
        assert(dismissed) { "onDismiss should be called when cancel is clicked" }
    }

    @Test
    fun confirmWithZeroAmount_showsError() {
        composeTestRule.setContent {
            BillTrackerTheme {
                AddTransactionDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithContentDescription("金额输入框").performTextInput("0")
        composeTestRule.onNodeWithContentDescription("添加账单").performClick()
        composeTestRule.onNodeWithText("请输入有效金额").assertIsDisplayed()
    }
}
