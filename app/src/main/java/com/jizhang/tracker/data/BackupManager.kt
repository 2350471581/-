package com.jizhang.tracker.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

@JsonClass(generateAdapter = true)
data class BackupTransaction(
    val dateMillis: Long,
    val amount: Double,
    val type: String,
    val source: String? = null,
    val category: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class BackupCustomPlan(
    val name: String,
    val target: Double,
    val note: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class BackupPlans(
    val balance: Double? = null,
    val todayPlanTarget: Double? = null,
    val totalPlanTarget: Double? = null,
    val savePlanTarget: Double? = null,
    val todayPlanNote: String? = null,
    val totalPlanNote: String? = null,
    val savePlanNote: String? = null,
    val customPlans: List<BackupCustomPlan>? = null
)

@JsonClass(generateAdapter = true)
data class BackupSettings(
    val nickname: String? = null,
    val themeIndex: Int? = null,
    val avatarEmoji: Int? = null,
    val customAvatarUri: String? = null,
    val customThemeConfigJson: String? = null,
    val aiChatEnabled: Boolean? = null,
    val isManualMode: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class BackupData(
    val version: String,
    val exportDate: Long,
    val transactions: List<BackupTransaction>,
    val plans: BackupPlans? = null,
    val settings: BackupSettings? = null,
    val checksum: String? = null
)

private val moshi: Moshi = Moshi.Builder().build()

object BackupManager {
    suspend fun exportToJson(context: Context, dao: TransactionDao, planStorage: PlanStorage): Uri? = withContext(Dispatchers.IO) {
        try {
            val allTx = dao.getTransactionsBetweenSync(0, Long.MAX_VALUE)
            val backup = BackupData(
                version = "0.8",
                exportDate = System.currentTimeMillis(),
                transactions = allTx.map { tx ->
                    BackupTransaction(
                        dateMillis = tx.dateMillis,
                        amount = tx.amount,
                        type = tx.type.name,
                        source = tx.source.name,
                        category = tx.category,
                        description = tx.description
                    )
                },
                plans = BackupPlans(
                    balance = planStorage.balance,
                    todayPlanTarget = planStorage.todayPlanTarget,
                    totalPlanTarget = planStorage.totalPlanTarget,
                    savePlanTarget = planStorage.savePlanTarget,
                    todayPlanNote = planStorage.todayPlanNote,
                    totalPlanNote = planStorage.totalPlanNote,
                    savePlanNote = planStorage.savePlanNote,
                    customPlans = planStorage.getAllCustomPlans().map { cp ->
                        BackupCustomPlan(
                            name = cp.name,
                            target = cp.target,
                            note = cp.note,
                            type = cp.type.name
                        )
                    }
                ),
                settings = BackupSettings(
                    nickname = planStorage.nickname,
                    themeIndex = planStorage.themeIndex,
                    avatarEmoji = planStorage.avatarEmoji,
                    customAvatarUri = planStorage.customAvatarUri,
                    customThemeConfigJson = planStorage.customThemeConfigJson,
                    aiChatEnabled = planStorage.aiChatEnabled,
                    isManualMode = planStorage.isManualMode
                )
            )
            val adapter = moshi.adapter(BackupData::class.java)
            // Serialize without checksum, compute SHA-256, then re-serialize with it
            val contentToSign = adapter.toJson(backup)
            val digest = MessageDigest.getInstance("SHA-256").digest(contentToSign.toByteArray())
            val hex = digest.joinToString("") { "%02x".format(it) }
            val finalJson = adapter.toJson(backup.copy(checksum = hex))
            val file = File(context.cacheDir, "billtracker_backup.json")
            file.writeText(finalJson)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("BackupManager", "Export failed", e)
            null
        }
    }

    suspend fun importFromJson(context: Context, dao: TransactionDao, planStorage: PlanStorage, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@withContext 0
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(content) ?: return@withContext 0

            // 校验 checksum（旧版备份无 checksum 仍接受）
            if (backup.checksum != null) {
                val unsigned = backup.copy(checksum = null)
                val contentToVerify = adapter.toJson(unsigned)
                val digest = MessageDigest.getInstance("SHA-256").digest(contentToVerify.toByteArray())
                val expected = digest.joinToString("") { "%02x".format(it) }
                if (expected != backup.checksum) {
                    Log.w("BackupManager", "Checksum mismatch — 文件可能被篡改")
                    return@withContext 0
                }
            }

            if (backup.version != "0.8") {
                Log.w("BackupManager", "Version mismatch: expected 0.8, got ${backup.version}")
            }

            val existing = dao.getTransactionsBetweenSync(0, Long.MAX_VALUE)
            val existingKeys = existing.mapTo(mutableSetOf()) {
                "${it.dateMillis}_${it.amount}_${it.type.name}_${it.description}"
            }

            var imported = 0
            backup.transactions.forEach { tx ->
                val key = "${tx.dateMillis}_${tx.amount}_${tx.type}_${tx.description.orEmpty()}"
                if (key !in existingKeys) {
                    dao.insert(
                        TransactionEntity(
                            dateMillis = tx.dateMillis,
                            amount = tx.amount,
                            type = try { TransactionType.valueOf(tx.type) } catch (_: Exception) { TransactionType.EXPENSE },
                            source = tx.source?.let { try { TransactionSource.valueOf(it) } catch (_: Exception) { null } } ?: TransactionSource.MANUAL,
                            category = tx.category ?: "其他",
                            description = tx.description.orEmpty()
                        )
                    )
                    imported++
                }
            }

            backup.plans?.let { p ->
                p.balance?.let { planStorage.balance = it }
                p.todayPlanTarget?.let { planStorage.todayPlanTarget = it }
                p.totalPlanTarget?.let { planStorage.totalPlanTarget = it }
                p.savePlanTarget?.let { planStorage.savePlanTarget = it }
                p.todayPlanNote?.let { planStorage.todayPlanNote = it }
                p.totalPlanNote?.let { planStorage.totalPlanNote = it }
                p.savePlanNote?.let { planStorage.savePlanNote = it }
                p.customPlans?.forEach { cp ->
                    planStorage.addCustomPlan(
                        CustomPlan(
                            name = cp.name,
                            target = cp.target,
                            note = cp.note.orEmpty(),
                            type = cp.type?.let { try { PlanDataType.valueOf(it) } catch (_: Exception) { null } } ?: PlanDataType.TODAY_NET
                        )
                    )
                }
            }

            backup.settings?.let { s ->
                s.nickname?.let { planStorage.nickname = it }
                s.themeIndex?.let { planStorage.themeIndex = it }
                s.avatarEmoji?.let { planStorage.avatarEmoji = it }
                s.customAvatarUri?.let { planStorage.customAvatarUri = it }
                s.customThemeConfigJson?.let { planStorage.customThemeConfigJson = it }
                s.aiChatEnabled?.let { planStorage.aiChatEnabled = it }
                s.isManualMode?.let { planStorage.isManualMode = it }
            }

            imported
        } catch (e: Exception) {
            Log.e("BackupManager", "Import failed", e)
            0
        }
    }
}
