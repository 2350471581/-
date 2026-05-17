package com.example.billtracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AIParseResult(
    val amount: Double,
    val type: TransactionType?,
    val category: String,
    val description: String
)

class AIBillService {

    private val apiKey = "sk-8014a71225a649c38a2a1bf28e314b05"
    private val apiUrl = "https://api.deepseek.com/chat/completions"

    suspend fun parse(text: String): AIParseResult? = withContext(Dispatchers.IO) {
        try {
            val result = callDeepSeek(text)
            if (result != null) return@withContext result
        } catch (_: Exception) { }
        // local fallback
        localParse(text)
    }

    private suspend fun callDeepSeek(text: String): AIParseResult? {
        val systemPrompt = """
你是一个记账助手。从用户输入的日常描述中提取账单信息，以JSON格式回复。
字段：
- amount: 金额（数字）
- type: "INCOME" 或 "EXPENSE"，如果无法判断收支方向请设为 null
- category: 分类（餐饮、交通、购物、生活缴费、娱乐、医疗、转账、工资、其他）
- description: 简要描述

无法识别时返回 {"error": "无法识别"}

示例：
{"amount": 35, "type": "EXPENSE", "category": "餐饮", "description": "午餐"}
{"amount": 8000, "type": "INCOME", "category": "工资", "description": "工资到账"}
{"amount": 500, "type": null, "category": "转账", "description": "转账500元"}
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray(
                listOf(
                    JSONObject().apply { put("role", "system"); put("content", systemPrompt) },
                    JSONObject().apply { put("role", "user"); put("content", text) }
                )
            ))
            put("temperature", 0.1)
            put("max_tokens", 256)
        }

        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        try {
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val responseCode = conn.responseCode
            if (responseCode != 200) return null

            val json = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            val obj = JSONObject(json)
            val content = obj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val jsonStr = if (content.startsWith("```")) {
                content.trimStart('`').substringAfter("json").trim().trimStart('`').trim()
            } else content

            val result = JSONObject(jsonStr)
            if (result.has("error")) return null

            val amount = result.optDouble("amount", -1.0)
            if (amount <= 0) return null

            val type = if (result.isNull("type")) null
                       else if (result.optString("type") == "INCOME") TransactionType.INCOME
                       else TransactionType.EXPENSE
            val category = result.optString("category", "其他")
            val description = result.optString("description", text.take(50))

            return AIParseResult(amount, type, category, description)
        } catch (e: Exception) {
            return null
        } finally {
            conn.disconnect()
        }
    }

    // AI 根据已添加账单生成上下文回复
    suspend fun chatWithContext(transaction: AIParseResult, summary: String): String? = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = "你是一个亲切的记账助手。用户刚添加了一笔账单，请根据用户的消费概况给出友好的回复。" +
                    "回复要简短亲切（50字以内），语气自然，可以包含简单的财务提醒或鼓励。" +
                    "不要使用markdown格式，不要加引号，直接说内容。"
            val userMsg = "刚添加的账单：${transaction.description}，${if (transaction.type == TransactionType.INCOME) "收入" else "支出"}${transaction.amount}元\n\n$summary"

            val body = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray(listOf(
                    JSONObject().apply { put("role", "system"); put("content", systemPrompt) },
                    JSONObject().apply { put("role", "user"); put("content", userMsg) }
                )))
                put("temperature", 0.7)
                put("max_tokens", 256)
            }

            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            try {
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                if (conn.responseCode != 200) return@withContext null
                val json = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val content = JSONObject(json).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message")
                    .getString("content").trim()
                content.removeSurrounding("\"").take(200)
            } catch (_: Exception) { null }
            finally { conn.disconnect() }
        } catch (_: Exception) { null }
    }

    private fun localParse(text: String): AIParseResult? {
        val amount = extractAmount(text) ?: return null
        val type = detectType(text)
        val category = detectCategory(text)
        return AIParseResult(amount, type, category, text.take(50))
    }

    private fun extractAmount(text: String): Double? {
        val patterns = listOf(
            Regex("""(\d+\.?\d*)\s*元"""),
            Regex("""(\d+\.?\d*)\s*块"""),
            Regex("""¥\s*(\d+\.?\d*)"""),
            Regex("""(\d+\.?\d*)"""),
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val v = m.groupValues[1].toDoubleOrNull() ?: continue
            if (v in 0.01..999999.0) return v
        }
        return null
    }

    private fun detectType(text: String): TransactionType? {
        val incomeWords = listOf("收入", "到账", "收到", "工资", "收款", "退款", "入账", "返现", "奖金")
        val expenseWords = listOf("花了", "消费", "支出", "付款", "买了", "支付", "缴费", "充值", "扣款", "转账", "借出", "借给")
        val income = incomeWords.any { text.contains(it) }
        val expense = expenseWords.any { text.contains(it) }
        return if (income && !expense) TransactionType.INCOME
        else if (expense && !income) TransactionType.EXPENSE
        else null // ambiguous, ask user
    }

    private fun detectCategory(text: String): String {
        val map = mapOf(
            "餐饮" to listOf("吃饭", "餐", "外卖", "午餐", "晚餐", "早餐", "食堂", "餐厅", "火锅", "奶茶", "咖啡", "饭店", "美食", "下馆子", "夜宵", "烧烤", "零食", "面包", "水果"),
            "交通" to listOf("打车", "地铁", "公交", "加油", "滴滴", "出租车", "高铁", "火车", "飞机", "停车", "过路费", "自行车", "充电"),
            "购物" to listOf("买了", "购物", "淘宝", "京东", "拼多多", "超市", "商场", "网购", "衣服", "鞋子", "数码", "日用品", "文具"),
            "生活缴费" to listOf("水电", "燃气", "物业", "话费", "网费", "宽带", "房租", "暖气", "有线电视"),
            "娱乐" to listOf("电影", "游戏", "KTV", "旅游", "景点", "门票", "视频会员", "音乐", "健身", "运动"),
            "医疗" to listOf("医院", "看病", "药", "体检", "医保", "诊所", "挂号"),
            "转账" to listOf("转账", "转给", "红包", "发红包", "借"),
            "工资" to listOf("工资", "薪水", "月薪", "薪水"),
            "退款" to listOf("退款", "退货"),
        )
        for ((cat, keywords) in map) {
            if (keywords.any { text.contains(it) }) return cat
        }
        return "其他"
    }
}
