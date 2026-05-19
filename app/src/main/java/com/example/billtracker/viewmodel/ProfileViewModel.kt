package com.example.billtracker.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.billtracker.data.BackupManager
import com.example.billtracker.data.PlanStorage
import com.example.billtracker.data.TransactionDao
import com.example.billtracker.data.TransactionSource
import com.example.billtracker.data.TransactionType
import com.example.billtracker.ui.CustomThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val planStorage: PlanStorage,
    private val dao: TransactionDao,
    application: Application
) : AndroidViewModel(application) {

    val themeIndex = MutableStateFlow(planStorage.themeIndex)
    val followSystemTheme = MutableStateFlow(planStorage.followSystemTheme)
    val nickname = MutableStateFlow(planStorage.nickname)
    val avatarEmoji = MutableStateFlow(planStorage.avatarEmoji)
    val customAvatarUri = MutableStateFlow(planStorage.customAvatarUri)
    val customThemeConfig = MutableStateFlow(
        CustomThemeConfig.fromJson(planStorage.customThemeConfigJson)
    )
    val aiChatEnabled = MutableStateFlow(planStorage.aiChatEnabled)
    val aiChatTutorialDone = MutableStateFlow(planStorage.aiChatTutorialDone)
    val isFirstLaunch = MutableStateFlow(planStorage.isFirstLaunch)

    fun setThemeIndex(index: Int) {
        themeIndex.value = index
        planStorage.themeIndex = index
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        followSystemTheme.value = enabled
        planStorage.followSystemTheme = enabled
    }

    fun setNickname(name: String) {
        nickname.value = name
        planStorage.nickname = name
    }

    fun setAvatarEmoji(index: Int) {
        avatarEmoji.value = index
        planStorage.avatarEmoji = index
    }

    fun setCustomAvatarUri(uri: String) {
        customAvatarUri.value = uri
        planStorage.customAvatarUri = uri
    }

    fun setCustomThemeConfig(config: CustomThemeConfig) {
        customThemeConfig.value = config
        planStorage.customThemeConfigJson = config.toJson()
    }

    fun setAiChatEnabled(enabled: Boolean) {
        aiChatEnabled.value = enabled
        planStorage.aiChatEnabled = enabled
    }

    fun markAiChatTutorialDone() {
        aiChatTutorialDone.value = true
        planStorage.aiChatTutorialDone = true
    }

    fun dismissFirstLaunch() {
        planStorage.markFirstLaunchSeen()
        isFirstLaunch.value = false
    }

    fun clearAllData() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    suspend fun exportCsv(startMillis: Long = 0, endMillis: Long = System.currentTimeMillis()): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTx = dao.getTransactionsBetweenSync(startMillis, endMillis)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val csv = buildString {
                appendLine("日期,金额,类型,来源,分类,备注")
                allTx.forEach { tx ->
                    val type = if (tx.type == TransactionType.INCOME) "收入" else "支出"
                    val source = when (tx.source) {
                        TransactionSource.WECHAT -> "微信"
                        TransactionSource.ALIPAY -> "支付宝"
                        TransactionSource.MANUAL -> "手动"
                        TransactionSource.BANK -> "银行"
                    }
                    appendLine("${dateFmt.format(tx.dateMillis)},${tx.amount},$type,$source,${tx.category},${tx.description.replace(",", "，")}")
                }
            }
            val file = File(getApplication<Application>().cacheDir, "billtracker_export.csv")
            file.writeText(csv)
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
        } catch (_: Exception) { null }
    }

    suspend fun exportImage(startMillis: Long = 0, endMillis: Long = System.currentTimeMillis()): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTx = dao.getTransactionsBetweenSync(startMillis, endMillis)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val width = 1080
            val headerHeight = 160
            val rowHeight = 44
            val padding = 60
            val titleHeight = 100
            val summaryHeight = 120
            val totalRows = allTx.size.coerceAtLeast(1)
            val height = padding + titleHeight + summaryHeight + headerHeight + totalRows * rowHeight + padding

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2C241A")
                textSize = 48f; typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#9AA0A6"); textSize = 26f
            }
            val rowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#3C4043"); textSize = 30f
            }
            val incomePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#4CAF7A"); textSize = 30f; typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val expensePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EA6B5C"); textSize = 30f; typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F1F3F4"); strokeWidth = 1f
            }
            val grayPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#9AA0A6"); textSize = 24f
            }

            var y = padding
            canvas.drawText("账单记录", 60f, y + 50f, titlePaint)
            y += titleHeight

            val totalIncome = allTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = allTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            canvas.drawText("收入: ¥${"%.2f".format(totalIncome)}", 60f, y.toFloat(), incomePaint)
            canvas.drawText("支出: ¥${"%.2f".format(totalExpense)}", 400f, y.toFloat(), expensePaint)
            y += summaryHeight

            canvas.drawRect(60f, y.toFloat(), (width - 60).toFloat(), (y + headerHeight).toFloat(), android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFF8F0")
            })
            canvas.drawText("日期", 80f, y + 36f, headerPaint)
            canvas.drawText("金额", 340f, y + 36f, headerPaint)
            canvas.drawText("分类", 540f, y + 36f, headerPaint)
            canvas.drawText("来源", 720f, y + 36f, headerPaint)
            canvas.drawText("备注", 860f, y + 36f, headerPaint)
            y += headerHeight

            val displayTx = allTx.take(100)
            displayTx.forEachIndexed { i, tx ->
                if (i % 2 == 1) {
                    canvas.drawRect(60f, y.toFloat(), (width - 60).toFloat(), (y + rowHeight).toFloat(), android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#F8F9FA")
                    })
                }
                val amountPaint = if (tx.type == TransactionType.INCOME) incomePaint else expensePaint
                val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                canvas.drawText(dateFmt.format(tx.dateMillis), 80f, y + 30f, grayPaint)
                canvas.drawText("${sign}¥${"%.2f".format(tx.amount)}", 340f, y + 30f, amountPaint)
                canvas.drawText(tx.category, 540f, y + 30f, rowPaint)
                val srcText = when (tx.source) {
                    TransactionSource.WECHAT -> "微信"; TransactionSource.ALIPAY -> "支付宝"
                    TransactionSource.MANUAL -> "手动"; TransactionSource.BANK -> "银行"
                }
                canvas.drawText(srcText, 720f, y + 30f, grayPaint)
                canvas.drawText(tx.description.take(8), 860f, y + 30f, rowPaint)
                y += rowHeight
                canvas.drawLine(60f, y.toFloat(), (width - 60).toFloat(), y.toFloat(), linePaint)
            }
            if (allTx.size > 50) {
                canvas.drawText("... 共${allTx.size}条记录", 60f, y + 40f, grayPaint)
            }

            val file = File(getApplication<Application>().cacheDir, "billtracker_export.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
        } catch (_: Exception) { null }
    }

    suspend fun exportBackup(): Uri? {
        return BackupManager.exportToJson(getApplication(), dao, planStorage)
    }

    suspend fun importBackup(uri: Uri): Int {
        return BackupManager.importFromJson(getApplication(), dao, planStorage, uri)
    }
}
