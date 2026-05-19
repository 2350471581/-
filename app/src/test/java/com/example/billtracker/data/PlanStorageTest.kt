package com.example.billtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlanStorageTest {
    private lateinit var storage: PlanStorage

    @Before
    fun setup() {
        storage = PlanStorage(InMemorySharedPreferences())
    }

    @Test
    fun `balance defaults to 0`() {
        assertEquals(0.0, storage.balance, 0.001)
    }

    @Test
    fun `balance round-trip`() {
        storage.balance = 1234.56
        assertEquals(1234.56, storage.balance, 0.001)
    }

    @Test
    fun `todayPlanTarget round-trip`() {
        storage.todayPlanTarget = 500.0
        assertEquals(500.0, storage.todayPlanTarget, 0.001)
    }

    @Test
    fun `totalPlanTarget round-trip`() {
        storage.totalPlanTarget = 10000.0
        assertEquals(10000.0, storage.totalPlanTarget, 0.001)
    }

    @Test
    fun `savePlanTarget round-trip`() {
        storage.savePlanTarget = 2000.0
        assertEquals(2000.0, storage.savePlanTarget, 0.001)
    }

    @Test
    fun `string fields round-trip`() {
        storage.todayPlanNote = "今日目标"
        assertEquals("今日目标", storage.todayPlanNote)
        storage.totalPlanNote = "总目标"
        assertEquals("总目标", storage.totalPlanNote)
        storage.savePlanNote = "存款目标"
        assertEquals("存款目标", storage.savePlanNote)
    }

    @Test
    fun `nickname defaults to user`() {
        assertEquals("用户", storage.nickname)
    }

    @Test
    fun `nickname round-trip`() {
        storage.nickname = "测试用户"
        assertEquals("测试用户", storage.nickname)
    }

    @Test
    fun `themeIndex defaults to 2`() {
        assertEquals(2, storage.themeIndex)
    }

    @Test
    fun `isManualMode defaults to true`() {
        assertTrue(storage.isManualMode)
    }

    @Test
    fun `customPlan CRUD`() {
        val plan = CustomPlan("旅行基金", 5000.0, "年底旅行", PlanDataType.TOTAL_NET)
        storage.addCustomPlan(plan)

        val plans = storage.getAllCustomPlans()
        assertEquals(1, plans.size)
        assertEquals("旅行基金", plans[0].name)
        assertEquals(5000.0, plans[0].target, 0.001)
        assertEquals("年底旅行", plans[0].note)
        assertEquals(PlanDataType.TOTAL_NET, plans[0].type)

        storage.updateCustomPlan(0, 6000.0, "欧洲旅行")
        val updated = storage.getCustomPlan(0)!!
        assertEquals(6000.0, updated.target, 0.001)
        assertEquals("欧洲旅行", updated.note)

        storage.deleteCustomPlan(0)
        assertTrue(storage.getAllCustomPlans().isEmpty())
    }

    @Test
    fun `installDateMillis is stable`() {
        val date1 = storage.installDateMillis
        val date2 = storage.installDateMillis
        assertEquals(date2, date1)
    }

    @Test
    fun `aiChatEnabled round-trip`() {
        storage.aiChatEnabled = true
        assertTrue(storage.aiChatEnabled)
        storage.aiChatEnabled = false
        assertFalse(storage.aiChatEnabled)
    }

    @Test
    fun `customThemeConfigJson round-trip`() {
        storage.customThemeConfigJson = """{"imageUri":"test"}"""
        assertEquals("""{"imageUri":"test"}""", storage.customThemeConfigJson)
    }

    @Test
    fun `deleteCustomPlan out of bounds does nothing`() {
        storage.deleteCustomPlan(99)
        assertTrue(storage.getAllCustomPlans().isEmpty())
    }

    @Test
    fun `updateCustomPlan out of bounds does nothing`() {
        storage.updateCustomPlan(99, 1000.0, "should not crash")
        assertTrue(storage.getAllCustomPlans().isEmpty())
    }
}
