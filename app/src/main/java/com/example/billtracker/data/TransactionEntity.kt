package com.example.billtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }
enum class TransactionSource { WECHAT, ALIPAY, MANUAL, BANK }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val source: TransactionSource,
    val description: String,
    val dateMillis: Long,
    val smsId: Long? = null,
    val category: String = "其他",
    val notificationTag: String? = null
)
