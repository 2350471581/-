package com.example.billtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.DeepSeekService
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val deepSeekService: DeepSeekService
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun generateMonthlySummary(
        year: Int, month: Int, totalIncome: Double, totalExpense: Double,
        netBalance: Double, expenseCategories: Map<String, Double>,
        transactionCount: Int, nickname: String = ""
    ): String = deepSeekService.generateMonthlySummary(
        year, month, totalIncome, totalExpense, netBalance,
        expenseCategories, transactionCount, nickname
    )
}
