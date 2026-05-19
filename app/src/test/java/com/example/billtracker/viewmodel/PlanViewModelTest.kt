package com.example.billtracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.billtracker.data.InMemorySharedPreferences
import com.example.billtracker.data.PlanStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlanViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private lateinit var viewModel: PlanViewModel

    @Before
    fun setup() {
        val storage = PlanStorage(InMemorySharedPreferences())
        viewModel = PlanViewModel(storage)
    }

    @Test
    fun `planBalance defaults to 0`() {
        assertEquals(0.0, viewModel.planBalance.value, 0.0)
    }

    @Test
    fun `updatePlanBalance updates value`() {
        viewModel.updatePlanBalance(5000.0)
        assertEquals(5000.0, viewModel.planBalance.value, 0.0)
    }

    @Test
    fun `todayPlanTarget round-trip`() {
        viewModel.updateTodayPlanTarget(300.0)
        assertEquals(300.0, viewModel.todayPlanTarget.value, 0.0)
    }

    @Test
    fun `totalPlanTarget round-trip`() {
        viewModel.updateTotalPlanTarget(10000.0)
        assertEquals(10000.0, viewModel.totalPlanTarget.value, 0.0)
    }

    @Test
    fun `savePlanTarget round-trip`() {
        viewModel.updateSavePlanTarget(2000.0)
        assertEquals(2000.0, viewModel.savePlanTarget.value, 0.0)
    }

    @Test
    fun `todayPlanNote round-trip`() {
        viewModel.updateTodayPlanNote("今日目标")
        assertEquals("今日目标", viewModel.todayPlanNote.value)
    }

    @Test
    fun `totalPlanNote round-trip`() {
        viewModel.updateTotalPlanNote("总目标")
        assertEquals("总目标", viewModel.totalPlanNote.value)
    }

    @Test
    fun `addCustomPlan adds to list`() {
        viewModel.addCustomPlan("旅行基金", 5000.0, "年底旅行")
        assertEquals(1, viewModel.customPlans.value.size)
        assertEquals("旅行基金", viewModel.customPlans.value[0].name)
    }

    @Test
    fun `updateCustomPlan modifies existing plan`() {
        viewModel.addCustomPlan("旅行基金", 5000.0, "年底旅行")
        viewModel.updateCustomPlan(0, 6000.0, "欧洲旅行")
        assertEquals(6000.0, viewModel.customPlans.value[0].target, 0.0)
        assertEquals("欧洲旅行", viewModel.customPlans.value[0].note)
    }

    @Test
    fun `deleteCustomPlan removes plan`() {
        viewModel.addCustomPlan("旅行基金", 5000.0, "年底旅行")
        viewModel.deleteCustomPlan(0)
        assertTrue(viewModel.customPlans.value.isEmpty())
    }

    @Test
    fun `multiple custom plans maintain order`() {
        viewModel.addCustomPlan("计划A", 100.0, "A")
        viewModel.addCustomPlan("计划B", 200.0, "B")
        viewModel.addCustomPlan("计划C", 300.0, "C")
        assertEquals(3, viewModel.customPlans.value.size)
        assertEquals("计划B", viewModel.customPlans.value[1].name)

        viewModel.deleteCustomPlan(1)
        assertEquals(2, viewModel.customPlans.value.size)
        assertEquals("计划C", viewModel.customPlans.value[1].name)
    }
}
