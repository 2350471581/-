package com.example.billtracker.data

data class SmsMessage(
    val id: Long,
    val body: String,
    val dateMillis: Long
)

object SmsParser {

    data class ParseResult(
        val amount: Double,
        val type: TransactionType,
        val source: TransactionSource
    )

    fun parse(sms: SmsMessage): ParseResult? {
        val body = sms.body.trim()

        val isWechat = body.contains("微信")
        val isAlipay = body.contains("支付宝")
        val isBank = detectBank(body)

        if (!isWechat && !isAlipay && !isBank) return null

        // 跳过理财/转账类消息
        if (body.contains("余额宝") || body.contains("余利宝") ||
            body.contains("基金") || body.contains("理财")
        ) return null

        val amount = extractAmount(body) ?: return null

        // 负号金额 → 强制为支出
        val forceExpense = hasNegativeAmount(body)

        if (isWechat) {
            val type = if (forceExpense) TransactionType.EXPENSE else determineWechatType(body)
            return ParseResult(amount, type, TransactionSource.WECHAT)
        }

        if (isAlipay) {
            val type = if (forceExpense) TransactionType.EXPENSE else determineAlipayType(body)
            return ParseResult(amount, type, TransactionSource.ALIPAY)
        }

        if (isBank) {
            val type = if (forceExpense) TransactionType.EXPENSE else determineBankType(body)
            return ParseResult(amount, type, TransactionSource.BANK)
        }

        return null
    }

    private fun hasNegativeAmount(text: String): Boolean {
        return Regex("""人民币-\d""").containsMatchIn(text) ||
               Regex("""[\$￥¥]-\d""").containsMatchIn(text)
    }

    private fun extractAmount(text: String): Double? {
        // 匹配 ¥100 / ￥100 / $100 格式
        val regex1 = Regex("""[\$￥¥](\d+(?:,\d{3})*(?:\.\d{1,2})?)""")
        regex1.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()?.let { return it }

        // 匹配 人民币10.20 / 人民币-10.20 格式（银行短信常用）
        val regexRmb = Regex("""人民币-?(\d+(?:\.\d{1,2})?)""")
        regexRmb.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

        // 匹配 100元 / 100.00元 格式
        val regex2 = Regex("""(\d+(?:\.\d{1,2})?)元""")
        regex2.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

        return null
    }

    private fun determineWechatType(body: String): TransactionType {
        // 收入关键词（先检查，避免"收到转账"误判为支出）
        val incomeKeywords = listOf("收款", "收入", "退款", "收到红包", "向你付款", "已退款", "收到转账", "入账")
        // 支出关键词
        val expenseKeywords = listOf("付款", "支出", "消费", "还款", "发送红包", "已成功支付", "自动缴费", "转账")

        for (kw in incomeKeywords) {
            if (body.contains(kw)) return TransactionType.INCOME
        }
        for (kw in expenseKeywords) {
            if (body.contains(kw)) return TransactionType.EXPENSE
        }

        return TransactionType.EXPENSE
    }

    private fun determineAlipayType(body: String): TransactionType {
        // 收入关键词
        val incomeKeywords = listOf("收入", "向你付款", "成功收款", "到账", "退款", "向你转账", "入账")
        // 支出关键词："向你收款" = 对方向你收款 = 你支出
        val expenseKeywords = listOf("向你收款", "付款", "支出", "消费", "还款", "自动缴费")

        // 注意："向你收款"是支出，"向你付款"是收入，"向XX收款"要看语境
        // "XX向你收款" → 支出（对方收你的钱）
        val xiangniShoukuan = Regex("""向你收款""")
        if (xiangniShoukuan.containsMatchIn(body)) return TransactionType.EXPENSE

        for (kw in incomeKeywords) {
            if (body.contains(kw)) return TransactionType.INCOME
        }
        for (kw in expenseKeywords) {
            if (body.contains(kw)) return TransactionType.EXPENSE
        }

        return TransactionType.EXPENSE
    }

    // ── 银行短信检测 ──
    private val bankNames = listOf(
        "工商银行", "工行", "建设银行", "建行", "农业银行", "农行",
        "中国银行", "招商银行", "招行", "交通银行", "交行",
        "邮储银行", "邮政储蓄", "浦发银行", "中信银行", "光大银行",
        "民生银行", "兴业银行", "平安银行", "华夏银行", "广发银行",
        "宁波银行", "上海银行", "北京银行", "微众银行", "网商银行",
        "农商银行", "农村信用社", "农村商业银行",
        "江苏银行", "杭州银行", "南京银行", "成都银行", "重庆银行",
        "长沙银行", "徽商银行", "郑州银行", "中原银行", "贵阳银行",
        "盛京银行", "天津银行", "大连银行", "青岛银行", "厦门银行",
        "广州银行", "东莞银行", "汉口银行", "恒丰银行", "浙商银行",
        "渤海银行", "花旗银行", "汇丰银行", "渣打银行", "东亚银行",
    )

    private fun detectBank(body: String): Boolean {
        return bankNames.any { body.contains(it) }
    }

    private fun determineBankType(body: String): TransactionType {
        // 收入关键词
        val incomeKeywords = listOf("收入", "存入", "工资", "汇款", "转入", "到账", "退款", "理财赎回", "入账")
        // 支出关键词
        val expenseKeywords = listOf("支出", "消费", "扣款", "取款", "取现", "缴费", "支付", "刷卡", "POS")

        // "转账"需要看语境：转入是收入，转出是支出
        if (body.contains("转账支出") || body.contains("转出")) return TransactionType.EXPENSE
        if (body.contains("转账收入") || body.contains("转入")) return TransactionType.INCOME

        for (kw in incomeKeywords) {
            if (body.contains(kw)) return TransactionType.INCOME
        }
        for (kw in expenseKeywords) {
            if (body.contains(kw)) return TransactionType.EXPENSE
        }

        return TransactionType.EXPENSE
    }
}
