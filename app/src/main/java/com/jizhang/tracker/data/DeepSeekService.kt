package com.jizhang.tracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekService @Inject constructor(
    private val deepSeekClient: DeepSeekClient
) {

    suspend fun generateMonthlySummary(
        year: Int,
        month: Int,
        totalIncome: Double,
        totalExpense: Double,
        netBalance: Double,
        expenseCategories: Map<String, Double>,
        transactionCount: Int,
        nickname: String = ""
    ): String = withContext(Dispatchers.IO) {
        val categoryDesc = expenseCategories.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (cat, amt) ->
                val pct = amt / totalExpense.coerceAtLeast(1.0) * 100
                "- $cat: ¥${"%.2f".format(amt)}（${"%.1f".format(pct)}%）"
            }

        val systemPrompt = "你是一个亲切活泼的理财助手，说话语气可爱俏皮，喜欢用表情符号和感叹号。请根据用户的月度账单数据，写一段简短温暖的总结评价（60-100字）。只说好的方面，多鼓励、多夸奖，不要有任何批评或建议。用第二人称'你'来称呼用户。语气要活泼亲切正能量！"

        val userPrompt = buildString {
            appendLine("这是${year}年${month}月的账单数据：")
            appendLine("总收入：¥${"%.2f".format(totalIncome)}")
            appendLine("总支出：¥${"%.2f".format(totalExpense)}")
            appendLine("净结余：¥${"%.2f".format(netBalance)}")
            appendLine("交易笔数：$transactionCount")
            if (expenseCategories.isNotEmpty()) {
                appendLine("\n支出分类：")
                append(categoryDesc)
            }
            if (nickname.isNotBlank()) {
                appendLine("\n用户昵称：$nickname")
            }
        }

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("max_tokens", 300)
            put("temperature", 0.9)
        }

        deepSeekClient.executeWithRetry(body) { it }.getOrThrow()
    }
}
