package com.example.billtracker

import com.example.billtracker.data.TransactionEntity
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType

fun createTestTransaction(
    id: Long = 0,
    amount: Double = 100.0,
    type: TransactionType = TransactionType.EXPENSE,
    source: TransactionSource = TransactionSource.MANUAL,
    description: String = "测试账单",
    dateMillis: Long = System.currentTimeMillis(),
    category: String = "其他"
) = TransactionEntity(
    id = id, amount = amount, type = type, source = source,
    description = description, dateMillis = dateMillis, category = category
)
