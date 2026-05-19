package com.jizhang.tracker.data

import com.jizhang.tracker.data.TransactionSource.MANUAL
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {

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

    suspend fun importFromSms(context: android.content.Context) {
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

    suspend fun addManualTransaction(amount: Double, type: TransactionType, description: String): Boolean {
        val now = System.currentTimeMillis()
        if (dao.countSimilarTransactions(amount, type.name, now) > 0) return false
        dao.insert(
            TransactionEntity(
                amount = amount,
                type = type,
                source = MANUAL,
                description = description,
                dateMillis = now
            )
        )
        return true
    }

    suspend fun importTransaction(bill: ParsedBill): Boolean {
        if (dao.countSimilarTransactions(bill.amount, bill.type.name, bill.dateMillis) > 0) return false
        dao.insert(
            TransactionEntity(
                amount = bill.amount,
                type = bill.type,
                source = bill.source,
                description = if (bill.description.isNotBlank()) "${bill.category} ${bill.description}" else bill.category,
                dateMillis = bill.dateMillis,
                category = bill.category
            )
        )
        return true
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
                    category = result.categoryName,
                    notificationTag = msg.tag
                )
            )
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
