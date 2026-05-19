package com.jizhang.tracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.jizhang.tracker.ui.BillTrackerTheme
import com.jizhang.tracker.data.TransactionEntity
import com.jizhang.tracker.data.TransactionSource
import com.jizhang.tracker.data.TransactionType
import org.junit.Rule
import org.junit.Test

class TransactionItemTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val sampleTransaction = TransactionEntity(
        id = 1,
        amount = 42.5,
        type = TransactionType.EXPENSE,
        source = TransactionSource.WECHAT,
        description = "午餐",
        dateMillis = System.currentTimeMillis(),
        category = "餐饮"
    )

    @Test
    fun displaysTransactionViaSemantics() {
        composeTestRule.setContent {
            BillTrackerTheme {
                TransactionItem(
                    transaction = sampleTransaction,
                    onDelete = {},
                    onItemClick = {}
                )
            }
        }
        // TransactionCard uses semantics contentDescription in format:
        // "${typeLabel}，¥{amount}，{category}，{source}"
        composeTestRule.onNodeWithContentDescription("支出，¥42.50，餐饮，微信").assertIsDisplayed()
    }
}
