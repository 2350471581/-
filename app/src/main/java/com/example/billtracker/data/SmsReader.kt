package com.example.billtracker.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsReader {
    suspend fun readRecentTransactions(context: Context): List<Pair<SmsMessage, SmsParser.ParseResult>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Pair<SmsMessage, SmsParser.ParseResult>>()
            val uri = Uri.parse("content://sms/inbox")

            // 只查最近90天的短信，并且只查含关键词的
            val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000

            val cursor = context.contentResolver.query(
                uri,
                arrayOf("_id", "body", "date"),
                "body LIKE '%微信%' OR body LIKE '%支付宝%' OR body LIKE '%银行%' OR body LIKE '%银联%' OR body LIKE '%入账%'",
                null,
                "date DESC"
            )

            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val body = c.getString(1) ?: continue
                    val date = c.getLong(2)

                    if (date < ninetyDaysAgo) break

                    val sms = SmsMessage(id, body, date)
                    val result = SmsParser.parse(sms)
                    if (result != null) {
                        results.add(sms to result)
                    }
                }
            }
            results
        }
    }
}
