package com.example.billtracker.data

import android.annotation.SuppressLint
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@SuppressLint("OverrideAbstract")
class NotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedTags = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        // 只处理微信和支付宝的通知
        if (!packageName.contains("com.tencent.mm") && !packageName.contains("com.eg.android.AlipayGphone")) {
            return
        }

        val notification = sbn.notification
        val title = notification.extras.getString("android.title") ?: return
        val text = notification.extras.getCharSequence("android.text")?.toString() ?: return

        // 跳过非支付相关通知
        if (!title.contains("微信") && !title.contains("支付宝") &&
            !text.contains("微信") && !text.contains("支付宝") &&
            !text.contains("收款") && !text.contains("付款") && !text.contains("到账") &&
            !text.contains("退款") && !text.contains("转账") && !text.contains("红包") &&
            !text.contains("消费") && !text.contains("支出")
        ) return

        val tag = "${sbn.packageName}:${sbn.tag ?: ""}:${sbn.id}"
        if (tag in processedTags) return
        processedTags.add(tag)
        // 限制缓存大小
        if (processedTags.size > 500) {
            processedTags.clear()
        }

        val msg = NotificationMessage(
            tag = tag,
            id = sbn.id,
            title = title,
            text = text,
            dateMillis = sbn.postTime
        )

        val result = NotificationParser.parse(msg)
        if (result != null) {
            scope.launch {
                val context = applicationContext
                val repository = TransactionRepository(context)
                repository.insertFromNotification(msg, result)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    companion object {
        fun isPermissionGranted(context: android.content.Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledPackages.contains(context.packageName)
        }
    }
}
