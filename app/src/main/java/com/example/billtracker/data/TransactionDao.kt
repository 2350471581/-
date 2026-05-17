package com.example.billtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT * FROM transactions
        WHERE dateMillis >= :dayStart AND dateMillis < :dayEnd
        ORDER BY dateMillis DESC
    """)
    fun getTransactionsByDay(dayStart: Long, dayEnd: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME' AND dateMillis >= :dayStart AND dateMillis < :dayEnd
    """)
    fun getDailyIncome(dayStart: Long, dayEnd: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE' AND dateMillis >= :dayStart AND dateMillis < :dayEnd
    """)
    fun getDailyExpense(dayStart: Long, dayEnd: Long): Flow<Double>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis BETWEEN :start AND :end ORDER BY dateMillis DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis BETWEEN :start AND :end ORDER BY dateMillis DESC")
    suspend fun getTransactionsBetweenSync(start: Long, end: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("SELECT COUNT(*) FROM transactions WHERE smsId = :smsId")
    suspend fun existsBySmsId(smsId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE notificationTag = :tag")
    suspend fun existsByNotificationTag(tag: String): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: Long, description: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
