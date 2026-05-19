package com.jizhang.tracker.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AIParseResult(
    val amount: Double,
    val type: TransactionType?,
    val category: String,
    val description: String
)

data class AIChatResult(
    val parseResult: AIParseResult?,
    val reply: String
)

class AIBillException(
    message: String,
    val isAuthError: Boolean = false,
    val isNetworkError: Boolean = false
) : Exception(message)

@Singleton
class AIBillService @Inject constructor(
    private val deepSeekClient: DeepSeekClient,
    private val planStorage: PlanStorage
) {
    companion object {
        private const val TAG = "AIBillService"
    }

    suspend fun parse(text: String, customCategories: List<CustomCategory> = emptyList()): Result<AIParseResult> = withContext(Dispatchers.IO) {
        try {
            val result = callDeepSeek(text, customCategories)
            if (result.isSuccess) return@withContext result
        } catch (e: Exception) {
            Log.w(TAG, "DeepSeek parse failed, falling back to local", e)
        }
        // local fallback
        val local = localParse(text, customCategories)
        if (local != null) Result.success(local)
        else Result.failure(AIBillException("无法识别账单信息，请提供金额和描述"))
    }

    private suspend fun callDeepSeek(text: String, customCategories: List<CustomCategory> = emptyList()): Result<AIParseResult> {
        val callCategories = if (customCategories.isNotEmpty()) {
            "餐饮、交通、购物、生活缴费、娱乐、医疗、转账、工资、住房、教育、退款、其他、${customCategories.joinToString("、") { it.name }}"
        } else "餐饮、交通、购物、生活缴费、娱乐、医疗、转账、工资、住房、教育、退款、其他"
        val customPrompt = planStorage.customAiPrompt
        val systemPrompt = if (customPrompt.isNotBlank()) {
            customPrompt
        } else """
你是一个记账助手。从用户输入的日常描述中提取账单信息，以JSON格式回复。
字段：
- amount: 金额（数字）
- type: "INCOME" 或 "EXPENSE"，如果无法判断收支方向请设为 null
- category: 分类（$callCategories）
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

        return deepSeekClient.executeWithRetry(body) { content ->
            val jsonStr = deepSeekClient.extractJson(content)
            val result = JSONObject(jsonStr)
            if (result.has("error")) return@executeWithRetry null

            val amount = result.optDouble("amount", -1.0)
            if (amount <= 0) return@executeWithRetry null

            val type = if (result.isNull("type")) null
                       else if (result.optString("type") == "INCOME") TransactionType.INCOME
                       else TransactionType.EXPENSE
            val category = result.optString("category", "其他")
            val description = result.optString("description", text.take(50))

            AIParseResult(amount, type, category, description)
        }
    }

    suspend fun parseChat(text: String, todaySummary: String = "", customCategories: List<CustomCategory> = emptyList()): Result<AIChatResult> = withContext(Dispatchers.IO) {
        try {
            val result = callDeepSeekChat(text, todaySummary, customCategories)
            if (result.isSuccess) return@withContext result
        } catch (_: Exception) { }

        // 本地兜底：用规则解析，生成自然回复
        val local = localParse(text, customCategories)
        if (local != null) {
            val reply = buildFallbackReply(local)
            return@withContext Result.success(AIChatResult(local, reply))
        }
        Result.failure(AIBillException("无法识别账单信息，请提供金额和收支描述"))
    }

    private fun buildFallbackReply(result: AIParseResult): String {
        val amountStr = "%.0f".format(result.amount).toDouble().toLong().toString()
        return when (result.type) {
            TransactionType.INCOME -> "记上了，${amountStr}元入账${result.category}"
            TransactionType.EXPENSE -> "记好了，${amountStr}元${result.category}"
            null -> "识别到${amountStr}元${result.category}，但不确定是收入还是支出"
        }
    }

    private suspend fun callDeepSeekChat(text: String, todaySummary: String, customCategories: List<CustomCategory>): Result<AIChatResult> {
        val chatCategories = if (customCategories.isNotEmpty()) {
            "餐饮、交通、购物、生活缴费、娱乐、医疗、转账、工资、住房、教育、退款、其他、${customCategories.joinToString("、") { it.name }}"
        } else "餐饮、交通、购物、生活缴费、娱乐、医疗、转账、工资、住房、教育、退款、其他"
        val systemPrompt = """
你是一个朋友式的记账助手，语气自然亲切，像朋友聊天一样。

从用户输入中提取账单信息，同时生成自然的对话式回复。

以JSON格式回复，包含两个字段：
- parse: 账单解析结果，如果完全无法识别设为 null
- reply: 对话式回复文字

parse字段：
- amount: 金额（数字）
- type: "INCOME" 或 "EXPENSE"，如果无法判断收支方向请设为 null
- category: 分类（$chatCategories）
- description: 简要描述

要求：
- reply 要像朋友聊天一样自然
- 适当时可以使用网络热门表情包文字和颜文字（如 😂🤣😭😍🥹🫡💀🙏👻 以及 (╥﹏╥) (｀∀´)Ψ (；′⌒`) (´▽`ʃ♡) 等等）
- 根据用户描述的情绪使用对应的emoji：开心用 🎉🥳✨，吃惊用 😱😲💀，花钱多了用 😭😅(╥﹏╥)
- **随机决定是否使用emoji/颜文字，平均每5条回复中约有1条包含即可，大部分回复不需要表情**
- 不要用"已识别""已记录"这类机械表述
- 根据金额大小给出不同反应（小支出轻松带过，大支出关心一下，收入替用户高兴）
- 简短亲切，不超过60字
- 无法识别时 reply 要像朋友问清楚，而不是机械地报错

示例：
输入："中午吃饭花了35块"
回复：{"parse": {"amount": 35, "type": "EXPENSE", "category": "餐饮", "description": "午餐"}, "reply": "好嘞，午饭35块记上了～今天吃的啥呀？😄"}

输入："工资到账8000元"
回复：{"parse": {"amount": 8000, "type": "INCOME", "category": "工资", "description": "工资到账"}, "reply": "哇工资到账了！8000块，这个月加油干💪🎉"}

输入："今天好开心"
回复：{"parse": null, "reply": "哈哈，有啥开心事呀？不过要是想记账的话，告诉我有多少钱干啥用了就行😄"}
        """.trimIndent()

        val userMsg = if (todaySummary.isNotBlank()) {
            "$text\n\n今日概况供参考：$todaySummary"
        } else text

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray(listOf(
                JSONObject().apply { put("role", "system"); put("content", systemPrompt) },
                JSONObject().apply { put("role", "user"); put("content", userMsg) }
            )))
            put("temperature", 0.8)
            put("max_tokens", 512)
        }

        return deepSeekClient.executeWithRetry(body) { content ->
            val jsonStr = deepSeekClient.extractJson(content)
            val result = JSONObject(jsonStr)
            val reply = result.optString("reply", "")

            if (result.isNull("parse") || !result.has("parse")) {
                return@executeWithRetry AIChatResult(null, reply.ifEmpty { "没太明白呢，跟我说花了多少或者收了多少钱就行～" })
            }

            val parseObj = result.getJSONObject("parse")
            val amount = parseObj.optDouble("amount", -1.0)
            if (amount <= 0) return@executeWithRetry AIChatResult(null, reply.ifEmpty { "金额没识别出来，再说清楚一点呗～" })

            val type = if (parseObj.isNull("type")) null
                       else if (parseObj.optString("type") == "INCOME") TransactionType.INCOME
                       else TransactionType.EXPENSE
            val category = parseObj.optString("category", "其他")
            val description = parseObj.optString("description", text.take(50))

            AIChatResult(AIParseResult(amount, type, category, description), reply)
        }
    }

    suspend fun chatWithContext(transaction: AIParseResult, summary: String): Result<String> = withContext(Dispatchers.IO) {
        val customPrompt = planStorage.customAiPrompt
        val systemPrompt = if (customPrompt.isNotBlank()) {
            customPrompt
        } else "你是一个亲切的记账助手。用户刚添加了一笔账单，请根据用户的消费概况给出友好的回复。" +
                "回复要简短亲切（50字以内），语气自然，可以包含简单的财务提醒或鼓励。" +
                "平均每5条回复中约1条使用表情包文字或颜文字(╥﹏╥)(´▽`ʃ♡)，随机决定是否使用。" +
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

        deepSeekClient.executeWithRetry(body) { content ->
            content.removeSurrounding("\"").take(200)
        }
    }

    // ── Local fallback parsing ──

    private fun localParse(text: String, customCategories: List<CustomCategory> = emptyList()): AIParseResult? {
        val amount = extractAmount(text) ?: return null
        val type = detectType(text)
        val category = detectCategory(text, customCategories)
        return AIParseResult(amount, type, category, text.take(50))
    }

    private fun extractAmount(text: String): Double? =
        AmountParser.firstValidOf(text, AmountParser.YUAN_SIMPLE, AmountParser.KUAI, AmountParser.CURRENCY_SIMPLE, AmountParser.BARE_NUMBER)

    private fun detectType(text: String): TransactionType? {
        val incomeWords = listOf("收入", "到账", "收到", "工资", "收款", "退款", "入账", "返现", "奖金")
        val expenseWords = listOf("花了", "消费", "支出", "付款", "买了", "支付", "缴费", "充值", "扣款", "转账", "借出", "借给")
        val income = incomeWords.any { text.contains(it) }
        val expense = expenseWords.any { text.contains(it) }
        return if (income && !expense) TransactionType.INCOME
        else if (expense && !income) TransactionType.EXPENSE
        else null
    }

    private fun detectCategory(text: String, customCategories: List<CustomCategory> = emptyList()): String {
        return TransactionCategory.detectWithCustom(text, customCategories)
    }
}
