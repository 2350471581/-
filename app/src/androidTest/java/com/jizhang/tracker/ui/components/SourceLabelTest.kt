package com.jizhang.tracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jizhang.tracker.ui.BillTrackerTheme
import com.jizhang.tracker.data.TransactionSource
import org.junit.Rule
import org.junit.Test

class SourceLabelTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun wechatSource_showsWechat() {
        composeTestRule.setContent {
            BillTrackerTheme {
                SourceLabel(source = TransactionSource.WECHAT)
            }
        }
        composeTestRule.onNodeWithText("微信").assertIsDisplayed()
    }

    @Test
    fun alipaySource_showsAlipay() {
        composeTestRule.setContent {
            BillTrackerTheme {
                SourceLabel(source = TransactionSource.ALIPAY)
            }
        }
        composeTestRule.onNodeWithText("支付宝").assertIsDisplayed()
    }

    @Test
    fun bankSource_showsBank() {
        composeTestRule.setContent {
            BillTrackerTheme {
                SourceLabel(source = TransactionSource.BANK)
            }
        }
        composeTestRule.onNodeWithText("银行").assertIsDisplayed()
    }

    @Test
    fun manualSource_showsOther() {
        composeTestRule.setContent {
            BillTrackerTheme {
                SourceLabel(source = TransactionSource.MANUAL)
            }
        }
        composeTestRule.onNodeWithText("其他").assertIsDisplayed()
    }
}
