package com.jizhang.tracker.data

/**
 * 统一金额解析工具。
 *
 * 所有金额提取的正则表达式集中定义在此，各解析器（SmsParser、
 * NotificationParser、BillImporter、AIBillService）组合使用。
 */
object AmountParser {

    const val MIN_AMOUNT = 0.01
    const val MAX_AMOUNT = 999999.0

    // ── 基础金额正则 ──

    /** 货币符号开头：¥100、￥100.00、$1,234.56 */
    val CURRENCY = Regex("""[\$￥¥](\d+(?:,\d{3})*(?:\.\d{1,2})?)""")
    /** 货币符号+可选空格+可选负号：¥-100、￥ 100.00 */
    val CURRENCY_FLEX = Regex("""[\$￥¥]\s?(-?\d+(?:,\d{3})*(?:\.\d{1,2})?)""")
    /** 宽松货币符号匹配：¥100 */
    val CURRENCY_SIMPLE = Regex("""[¥￥]\s*(\d+\.?\d*)""")
    /** 人民币前缀：人民币10.20、人民币-10.20（银行短信常用） */
    val RMB_PREFIX = Regex("""人民币-?(\d+(?:\.\d{1,2})?)""")
    /** 元后缀（严格）：100.00元 */
    val YUAN = Regex("""(\d+(?:\.\d{1,2})?)元""")
    /** 元后缀（宽松）：100元 */
    val YUAN_SIMPLE = Regex("""(\d+\.?\d*)\s*元""")
    /** 块后缀：25块（口语） */
    val KUAI = Regex("""(\d+\.?\d*)\s*块""")
    /** 到账前缀：到账 XXX.XX 元 */
    val DAOZHANG = Regex("""到账[^0-9]*(\d+(?:\.\d{1,2})?)""")
    /** 裸数字兜底 */
    val BARE_NUMBER = Regex("""(\d+\.?\d*)""")

    // ── OCR 支付关键词模式 ──

    val PAYMENT_KEYWORDS = listOf("实付", "合计", "总计", "金额", "收款", "小计").map { kw ->
        Regex("""$kw[：:\s]*[¥￥]?\s*(\d+\.?\d*)""")
    }

    // ── 上下文模式（用于 extractAmountWithType）──

    val EXPENSE_CONTEXT = Regex("""(?:支出|付款|消费|扣款|支付).*?[-−]\s*[¥￥]?\s*(\d+\.?\d*)""")
    val INCOME_CONTEXT = Regex("""(?:收入|收款|转入|退款|到账).*?[+＋]?\s*[¥￥]?\s*(\d+\.?\d*)""")
    val STANDALONE_MINUS = Regex("""(?:^|\s)[-−]\s*[¥￥]?\s*(\d+\.?\d{2})\s*$""", RegexOption.MULTILINE)
    val PLUS_SIGN = Regex("""[+＋]\s*[¥￥]?\s*(\d+\.?\d*)""")

    // ── 负号检测模式 ──

    val NEGATIVE_BANK = Regex("""人民币-\d""")
    val NEGATIVE_CURRENCY = Regex("""[\$￥¥]-\d""")

    // ── 辅助函数 ──

    /** 去除千分位逗号 */
    fun stripCommas(match: MatchResult): Double? =
        match.groupValues[1].replace(",", "").toDoubleOrNull()

    /** 金额是否在有效范围内 */
    fun valid(v: Double) = v in MIN_AMOUNT..MAX_AMOUNT

    // ── 策略函数 ──

    /** 依次尝试多个模式，返回第一个匹配 */
    fun firstOf(text: String, vararg patterns: Regex): Double? {
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val v = stripCommas(m) ?: continue
            return v
        }
        return null
    }

    /** 依次尝试多个模式，返回第一个匹配且范围有效的金额 */
    fun firstValidOf(text: String, vararg patterns: Regex): Double? {
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val v = stripCommas(m) ?: continue
            if (valid(v)) return v
        }
        return null
    }

    /** 灵活货币模式（处理负号取绝对值） */
    fun currencyFlex(text: String): Double? {
        val m = CURRENCY_FLEX.find(text) ?: return null
        val v = stripCommas(m) ?: return null
        return if (v < 0) -v else v
    }

    /** OCR 支付关键词提取（实付/合计/总计/金额/收款/小计） */
    fun paymentKeyword(text: String): Double? {
        for (p in PAYMENT_KEYWORDS) {
            val m = p.find(text) ?: continue
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && valid(v)) return v
        }
        return null
    }

    /** 兜底：取最后一个匹配的符号金额（OCR 场景通常最后一个是合计） */
    fun lastSymbolAmount(text: String): Double? {
        for (p in listOf(CURRENCY_SIMPLE, YUAN_SIMPLE)) {
            val matches = p.findAll(text).toList()
            if (matches.isNotEmpty()) {
                val best = matches.lastOrNull() ?: matches.first()
                val v = best.groupValues[1].toDoubleOrNull()
                if (v != null && valid(v)) return v
            }
        }
        return null
    }

    /** 检测负号金额指示 */
    fun hasNegative(text: String): Boolean =
        NEGATIVE_BANK.containsMatchIn(text) || NEGATIVE_CURRENCY.containsMatchIn(text)
}
