package com.example.billtracker.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                if (!body.contains("微信") && !body.contains("支付宝")) continue

                val smsMessage = SmsMessage(
                    id = System.currentTimeMillis() % 100000,
                    body = body,
                    dateMillis = System.currentTimeMillis()
                )

                val result = SmsParser.parse(smsMessage)
                if (result != null) {
                    val receiver = SmsReceiverCallback ?: return
                    receiver.onNewTransaction(smsMessage, result)
                }
            }
        }
    }

    companion object {
        var SmsReceiverCallback: SmsReceiverCallback? = null
    }

    interface SmsReceiverCallback {
        fun onNewTransaction(sms: SmsMessage, result: SmsParser.ParseResult)
    }
}
