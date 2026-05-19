package com.example.billtracker.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.billtracker.data.AIBillService
import com.example.billtracker.data.NotificationListener
import com.example.billtracker.data.PlanStorage
import com.example.billtracker.data.SmsParser
import com.example.billtracker.data.SmsMessage
import com.example.billtracker.data.TransactionDao
import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionRepository
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.data.ParsedBill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val planStorage: PlanStorage,
    private val dao: TransactionDao,
    val aiBillService: AIBillService,
    application: Application
) : AndroidViewModel(application) {

    private val todayStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val todayEnd: Long
        get() = todayStart + 86400000L

    val todayTransactions: StateFlow<List<TransactionEntity>> =
        repository.getTodayTransactions(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayIncome: StateFlow<Double> =
        repository.getTodayIncome(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayExpense: StateFlow<Double> =
        repository.getTodayExpense(todayStart, todayEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTransactions: StateFlow<List<TransactionEntity>> =
        repository.getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRefreshing = MutableStateFlow(false)

    val installDateMillis = planStorage.installDateMillis

    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    val recentTransactions: StateFlow<List<TransactionEntity>> =
        combine(allTransactions, filterStartDate, filterEndDate) { list, start, end ->
            list.filter { it.dateMillis >= installDateMillis }.let { filtered ->
                val s = start ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                val e = end ?: Long.MAX_VALUE
                filtered.filter { it.dateMillis in s..e }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isManualMode = MutableStateFlow(planStorage.isManualMode)

    val hasNotificationPermission = MutableStateFlow(
        NotificationListener.isPermissionGranted(application)
    )

    fun setManualMode(enabled: Boolean) {
        isManualMode.value = enabled
        planStorage.isManualMode = enabled
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun checkNotificationPermission() {
        hasNotificationPermission.value = NotificationListener.isPermissionGranted(getApplication())
    }

    fun addTransaction(amount: Double, type: TransactionType, description: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.addManualTransaction(amount, type, description)
            onResult(success)
        }
    }

    fun importBills(bills: List<ParsedBill>, onDuplicateCount: (Int) -> Unit = {}) {
        viewModelScope.launch {
            var duplicates = 0
            bills.forEach {
                if (!repository.importTransaction(it)) duplicates++
            }
            onDuplicateCount(duplicates)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateTransactionNote(id: Long, note: String) {
        viewModelScope.launch {
            repository.updateTransactionDescription(id, note)
        }
    }

    fun addTestTransaction() {
        viewModelScope.launch {
            val testData = arrayOf(
                Triple("微信支付", "收款", TransactionType.INCOME),
                Triple("微信支付", "消费", TransactionType.EXPENSE),
                Triple("微信支付", "付款", TransactionType.EXPENSE),
                Triple("微信支付", "退款", TransactionType.INCOME),
                Triple("支付宝", "收入", TransactionType.INCOME),
                Triple("支付宝", "向你收款", TransactionType.EXPENSE),
                Triple("支付宝", "向你付款", TransactionType.INCOME),
                Triple("支付宝", "消费", TransactionType.EXPENSE),
            )
            val (sender, action, type) = testData.random()
            val amount = (1..100).random().toDouble()
            val source = if (sender.contains("微信")) TransactionSource.WECHAT else TransactionSource.ALIPAY
            val body = "【$sender】${action}¥${"%.2f".format(amount)}"

            repository.insertFromReceiver(
                SmsMessage(System.currentTimeMillis(), body, System.currentTimeMillis()),
                SmsParser.ParseResult(amount, type, source)
            )
        }
    }

    fun refreshFromSms() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.importFromSms(getApplication())
            isRefreshing.value = false
        }
    }

    suspend fun getTodaySummary(): String = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val end = start + 86400000L

        val tx = dao.getTransactionsBetweenSync(start, end)
        val income = tx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = tx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val count = tx.size

        buildString {
            appendLine("今日账单概况（截至${"HH:mm".let { SimpleDateFormat(it, Locale.getDefault()).format(Date()) }}）：")
            appendLine("收入：¥${"%.2f".format(income)}")
            appendLine("支出：¥${"%.2f".format(expense)}")
            appendLine("交易笔数：$count")
            if (tx.isNotEmpty()) {
                append("最近几笔：")
                tx.take(5).forEachIndexed { i, t ->
                    val type = if (t.type == TransactionType.INCOME) "收入" else "支出"
                    append("${if (i > 0) "；" else ""}${t.description} ${type}¥${"%.2f".format(t.amount)}")
                }
            }
        }
    }
}
