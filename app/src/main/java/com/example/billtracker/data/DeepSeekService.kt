package com.example.billtracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeepSeekService {

    private const val API_KEY = "sk-8014a71225a649c38a2a1bf28e314b05"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://api.deepseek.com/chat/completions"

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

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (!response.isSuccessful || responseBody == null) {
            throw Exception("API 请求失败: ${response.code}")
        }

        val json = JSONObject(responseBody)
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    suspend fun checkApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 5)
            }
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
