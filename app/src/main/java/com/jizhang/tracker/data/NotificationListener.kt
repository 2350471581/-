package com.jizhang.tracker.data

import android.annotation.SuppressLint
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@SuppressLint("OverrideAbstract")
class NotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedTags = mutableSetOf<String>()

    override fun onListenerConnected() {
        Log.i("NotificationListener", "监听服务已连接")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handleNotificationPosted(sbn)
    }

    @SuppressLint("OverrideAbstract")
    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: android.service.notification.NotificationListenerService.RankingMap?) {
        handleNotificationPosted(sbn)
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        // 只处理微信和支付宝的通知
        if (!packageName.contains("com.tencent.mm") && !packageName.contains("com.eg.android.AlipayGphone")) {
            return
        }

        val notification = sbn.notification
        val title = notification.extras.getString("android.title") ?: return
        val text = notification.extras.getCharSequence("android.text")?.toString() ?: return

        val triggerKeywords = applicationContext
            .getSharedPreferences("plan_prefs", android.content.Context.MODE_PRIVATE)
            .getStringSet("trigger_keywords", setOf("微信", "支付宝")) ?: setOf("微信", "支付宝")

        // 跳过非支付相关通知
        val hasPaymentKeyword = title.contains("微信") || title.contains("支付宝") ||
            text.contains("微信") || text.contains("支付宝") ||
            text.contains("收款") || text.contains("付款") || text.contains("到账") ||
            text.contains("退款") || text.contains("转账") || text.contains("红包") ||
            text.contains("消费") || text.contains("支出") ||
            triggerKeywords.any { text.contains(it) || title.contains(it) }
        if (!hasPaymentKeyword) return

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

        val customCategoriesJson = applicationContext
            .getSharedPreferences("plan_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_categories", null)
        val customCategories = if (customCategoriesJson != null) {
            try {
                val arr = org.json.JSONArray(customCategoriesJson)
                (0 until arr.length()).map {
                    val obj = arr.getJSONObject(it)
                    CustomCategory(
                        name = obj.getString("name"),
                        icon = obj.optString("icon", "📌"),
                        keywords = obj.optJSONArray("keywords")?.let { arr2 ->
                            (0 until arr2.length()).map { arr2.getString(it) }
                        } ?: emptyList()
                    )
                }
            } catch (_: Exception) { emptyList<CustomCategory>() }
        } else emptyList()

        val result = NotificationParser.parse(msg, customCategories = customCategories)
        if (result != null) {
            scope.launch {
                val context = applicationContext
                val entryPoint = EntryPointAccessors.fromApplication(context, NotificationEntryPoint::class.java)
                val repository = entryPoint.repository
                repository.insertFromNotification(msg, result)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        fun isPermissionGranted(context: android.content.Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledPackages.contains(context.packageName)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    val repository: TransactionRepository
}
