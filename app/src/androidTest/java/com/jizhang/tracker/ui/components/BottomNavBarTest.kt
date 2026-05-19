package com.jizhang.tracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jizhang.tracker.ui.BillTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BottomNavBarTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun tabs_areDisplayed() {
        composeTestRule.setContent {
            BillTrackerTheme {
                BillTrackerBottomBar(selectedTab = 0, onTabSelected = {})
            }
        }
        composeTestRule.onNodeWithContentDescription("记账 Tab").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("计划 Tab").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("分析 Tab").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("我的 Tab").assertIsDisplayed()
    }

    @Test
    fun tabSelection_triggersCallback() {
        var selectedIndex = -1
        composeTestRule.setContent {
            BillTrackerTheme {
                BillTrackerBottomBar(selectedTab = 0, onTabSelected = { selectedIndex = it })
            }
        }
        composeTestRule.onNodeWithContentDescription("计划 Tab").performClick()
        assertEquals(1, selectedIndex)
    }

    @Test
    fun selectedTab_showsLabelText() {
        composeTestRule.setContent {
            BillTrackerTheme {
                BillTrackerBottomBar(selectedTab = 1, onTabSelected = {})
            }
        }
        composeTestRule.onNodeWithText("计划").assertIsDisplayed()
    }
}
