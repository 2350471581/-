package com.jizhang.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jizhang.tracker.data.CustomPlan
import com.jizhang.tracker.data.PlanDataType
import com.jizhang.tracker.data.PlanStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planStorage: PlanStorage
) : ViewModel() {

    val planBalance = MutableStateFlow(planStorage.balance)
    val todayPlanTarget = MutableStateFlow(planStorage.todayPlanTarget)
    val totalPlanTarget = MutableStateFlow(planStorage.totalPlanTarget)
    val savePlanTarget = MutableStateFlow(planStorage.savePlanTarget)
    val todayPlanNote = MutableStateFlow(planStorage.todayPlanNote)
    val totalPlanNote = MutableStateFlow(planStorage.totalPlanNote)
    val savePlanNote = MutableStateFlow(planStorage.savePlanNote)
    val customPlans = MutableStateFlow(planStorage.getAllCustomPlans())

    fun updatePlanBalance(balance: Double) {
        planBalance.value = balance
        planStorage.balance = balance
    }

    fun updateTodayPlanTarget(target: Double) {
        todayPlanTarget.value = target
        planStorage.todayPlanTarget = target
    }

    fun updateTotalPlanTarget(target: Double) {
        totalPlanTarget.value = target
        planStorage.totalPlanTarget = target
    }

    fun updateTodayPlanNote(note: String) {
        todayPlanNote.value = note
        planStorage.todayPlanNote = note
    }

    fun updateTotalPlanNote(note: String) {
        totalPlanNote.value = note
        planStorage.totalPlanNote = note
    }

    fun updateSavePlanTarget(target: Double) {
        savePlanTarget.value = target
        planStorage.savePlanTarget = target
    }

    fun updateSavePlanNote(note: String) {
        savePlanNote.value = note
        planStorage.savePlanNote = note
    }

    fun addCustomPlan(name: String, target: Double, note: String, type: PlanDataType = PlanDataType.TODAY_NET) {
        val plan = CustomPlan(name, target, note, type)
        planStorage.addCustomPlan(plan)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    fun updateCustomPlan(index: Int, target: Double, note: String) {
        planStorage.updateCustomPlan(index, target, note)
        customPlans.value = planStorage.getAllCustomPlans()
    }

    fun deleteCustomPlan(index: Int) {
        planStorage.deleteCustomPlan(index)
        customPlans.value = planStorage.getAllCustomPlans()
    }
}
