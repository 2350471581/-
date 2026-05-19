package com.jizhang.tracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.jizhang.tracker.ui.BillTrackerTheme
import org.junit.Rule
import org.junit.Test

class PrivacyFooterTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun privacyFooter_displaysCorrectContent() {
        composeTestRule.setContent {
            BillTrackerTheme {
                PrivacyFooter()
            }
        }
        composeTestRule.onNodeWithContentDescription("你的隐私数据仅保存在本地，不会上传").assertIsDisplayed()
    }
}
