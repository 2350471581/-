package com.jizhang.tracker.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("manual_mode", true)) return

        val triggerKeywords = prefs.getStringSet("trigger_keywords", setOf("微信", "支付宝"))
            ?: setOf("微信", "支付宝")

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val baseId = System.currentTimeMillis()
        for ((i, sms) in messages.withIndex()) {
                val body = sms.messageBody ?: continue
                if (!triggerKeywords.any { body.contains(it) }) continue

                val smsMessage = SmsMessage(
                    // 使用负数 ID 避免与系统短信 _id 碰撞
                    id = -(baseId + i),
                    body = body,
                    dateMillis = baseId + i
                )

                val result = SmsParser.parse(smsMessage, triggerKeywords) ?: continue

                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            SmsReceiverEntryPoint::class.java
                        )
                        val repository = entryPoint.repository
                        repository.insertFromReceiver(smsMessage, result)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    val repository: TransactionRepository
}
