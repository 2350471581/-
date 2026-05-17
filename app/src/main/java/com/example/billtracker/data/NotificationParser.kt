package com.example.billtracker.data

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
    val category: TransactionCategory
)

object NotificationParser {

    fun parse(notification: NotificationMessage): NotificationParseResult? {
        val text = notification.text.trim()
        val title = notification.title.trim()

        val isWechat = title.contains("微信") || text.contains("微信")
        val isAlipay = title.contains("支付宝") || text.contains("支付宝")

        if (!isWechat && !isAlipay) return null

        val amount = extractAmount(text) ?: return null
        val source = if (isWechat) TransactionSource.WECHAT else TransactionSource.ALIPAY
        val type = determineType(source, text)
        val category = TransactionCategory.detect(text)

        return NotificationParseResult(amount, type, source, category)
    }

    private fun extractAmount(text: String): Double? {
        // 匹配 ¥100.00 / ¥100 / ¥ -100 等格式
        val regex1 = Regex("""[\$￥¥]\s?(-?\d+(?:,\d{3})*(?:\.\d{1,2})?)""")
        regex1.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let {
            return if (it < 0) -it else it
        }

        // 匹配 "到账 XXX.XX 元" 格式
        val regex2 = Regex("""到账[^0-9]*(\d+(?:\.\d{1,2})?)""")
        regex2.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

        // 匹配 100.00元 格式
        val regex3 = Regex("""(\d+(?:\.\d{1,2})?)元""")
        regex3.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

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
