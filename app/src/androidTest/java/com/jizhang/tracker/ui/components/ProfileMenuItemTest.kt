package com.jizhang.tracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.jizhang.tracker.ui.BillTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileMenuItemTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun menuItem_displaysLabel() {
        composeTestRule.setContent {
            BillTrackerTheme {
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    label = "测试设置",
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("测试设置").assertIsDisplayed()
    }

    @Test
    fun click_triggersOnClick() {
        var clicked = false
        composeTestRule.setContent {
            BillTrackerTheme {
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    label = "测试设置",
                    onClick = { clicked = true }
                )
            }
        }
        composeTestRule.onNodeWithText("测试设置").performClick()
        assertTrue("onClick should be triggered", clicked)
    }
}
