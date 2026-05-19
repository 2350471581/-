package com.jizhang.tracker.data

data class NotificationMessage(
    val tag: String,
    val id: Int,
    val title: String,
    val text: String,
    val dateMillis: Long
)

data class NotificationParseResult(
    val amount: Double,
    val type: TransactionType,
    val source: TransactionSource,
    val category: TransactionCategory,
    val categoryName: String = category.displayName
)

object NotificationParser {

    fun parse(notification: NotificationMessage, triggerKeywords: Set<String> = emptySet(), customCategories: List<CustomCategory> = emptyList()): NotificationParseResult? {
        val text = notification.text.trim()
        val title = notification.title.trim()

        val isWechat = title.contains("微信") || text.contains("微信")
        val isAlipay = title.contains("支付宝") || text.contains("支付宝")
        val isTriggered = isWechat || isAlipay || triggerKeywords.any { text.contains(it) || title.contains(it) }

        if (!isTriggered) return null

        val amount = extractAmount(text) ?: return null
        val source = when {
            isWechat -> TransactionSource.WECHAT
            isAlipay -> TransactionSource.ALIPAY
            else -> TransactionSource.UNKNOWN
        }
        val type = determineType(source, text)
        val category = TransactionCategory.detect(text)
        val categoryName = TransactionCategory.detectWithCustom(text, customCategories)

        return NotificationParseResult(amount, type, source, category, categoryName)
    }

    private fun extractAmount(text: String): Double? {
        // 货币符号（处理负号取绝对值）
        AmountParser.currencyFlex(text)?.let { return it }
        // 到账前缀
        AmountParser.firstOf(text, AmountParser.DAOZHANG)?.let { return it }
        // 元后缀
        AmountParser.firstOf(text, AmountParser.YUAN)?.let { return it }
        return null
    }

    private fun determineType(source: TransactionSource, text: String): TransactionType {
        val incomeKeywords = listOf("收款", "收入", "到账", "退款", "收到", "入账", "向你付款", "转账收入")
        val expenseKeywords = listOf("付款", "支出", "消费", "向你收款", "还款", "缴费", "转账支出",
            "发送红包", "已成功支付", "自动缴费", "扣款", "购买")

        // 退款永远是收入
        if (text.contains("退款")) return TransactionType.INCOME

        if (source == TransactionSource.WECHAT) {
            // 微信："向你收款" → 你支出
            if (text.contains("向你收款")) return TransactionType.EXPENSE
            // 微信："向你付款" → 你收入
            if (text.contains("向你付款")) return TransactionType.INCOME
        }

        if (source == TransactionSource.ALIPAY) {
            if (text.contains("向你收款")) return TransactionType.EXPENSE
            if (text.contains("向你付款")) return TransactionType.INCOME
        }

        // 负号金额 → 支出
        if (Regex("""-\d""").containsMatchIn(text)) return TransactionType.EXPENSE

        for (kw in incomeKeywords) {
            if (text.contains(kw)) return TransactionType.INCOME
        }
        for (kw in expenseKeywords) {
            if (text.contains(kw)) return TransactionType.EXPENSE
        }

        return TransactionType.EXPENSE
    }
}
