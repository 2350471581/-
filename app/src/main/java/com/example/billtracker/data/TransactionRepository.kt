package com.example.billtracker.data

import android.content.Context
import com.example.billtracker.data.TransactionSource.MANUAL
import com.example.billtracker.data.TransactionSource.WECHAT
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).transactionDao()

    fun getTodayTransactions(dayStart: Long, dayEnd: Long): Flow<List<TransactionEntity>> {
        return dao.getTransactionsByDay(dayStart, dayEnd)
    }

    fun getTodayIncome(dayStart: Long, dayEnd: Long): Flow<Double> {
        return dao.getDailyIncome(dayStart, dayEnd)
    }

    fun getTodayExpense(dayStart: Long, dayEnd: Long): Flow<Double> {
        return dao.getDailyExpense(dayStart, dayEnd)
    }

    fun getAllTransactions(): Flow<List<TransactionEntity>> {
        return dao.getAllTransactions()
    }

    suspend fun importFromSms() {
        val smsResults = SmsReader.readRecentTransactions(context)

        for ((sms, parsed) in smsResults) {
            val exists = dao.existsBySmsId(sms.id)
            if (exists == 0) {
                dao.insert(
                    TransactionEntity(
                        amount = parsed.amount,
                        type = parsed.type,
                        source = parsed.source,
                        description = sms.body.take(100),
                        dateMillis = sms.dateMillis,
                        smsId = sms.id
                    )
                )
            }
        }
    }

    suspend fun addManualTransaction(amount: Double, type: TransactionType, description: String) {
        dao.insert(
            TransactionEntity(
                amount = amount,
                type = type,
                source = MANUAL,
                description = description,
                dateMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteById(id)
    }

    suspend fun updateTransactionDescription(id: Long, description: String) {
        dao.updateDescription(id, description)
    }

    suspend fun insertFromReceiver(sms: SmsMessage, result: SmsParser.ParseResult) {
        val exists = dao.existsBySmsId(sms.id)
        if (exists == 0) {
            dao.insert(
                TransactionEntity(
                    amount = result.amount,
                    type = result.type,
                    source = result.source,
                    description = sms.body.take(100),
                    dateMillis = sms.dateMillis,
                    smsId = sms.id
                )
            )
        }
    }

    suspend fun insertFromNotification(msg: NotificationMessage, result: NotificationParseResult) {
        val exists = dao.existsByNotificationTag(msg.tag)
        if (exists == 0) {
            dao.insert(
                TransactionEntity(
                    amount = result.amount,
                    type = result.type,
                    source = result.source,
                    description = msg.text.take(100),
                    dateMillis = msg.dateMillis,
                    category = result.category.displayName,
                    notificationTag = msg.tag
                )
            )
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
