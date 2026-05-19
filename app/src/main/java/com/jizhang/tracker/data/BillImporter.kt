package com.jizhang.tracker.data

import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedBill(
    val dateMillis: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "其他",
    val description: String = "",
    val source: TransactionSource = TransactionSource.MANUAL
)

object BillImporter {

    private val dateFormats = listOf(
        "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
        "MM-dd", "MM/dd",
        "yyyy年MM月dd日", "MM月dd日"
    )

    private fun detectPaymentSource(text: String): TransactionSource {
        val t = text.lowercase().replace(" ", "")
        return when {
            t.contains("微信") || t.contains("wechat") -> TransactionSource.WECHAT
            t.contains("支付宝") || t.contains("alipay") -> TransactionSource.ALIPAY
            t.contains("银行") || t.contains("bank") || t.contains("银行卡") -> TransactionSource.BANK
            else -> TransactionSource.MANUAL
        }
    }

    fun parseCsv(content: String): List<ParsedBill> {
        val lines = content.trim().lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val headerIndex = findHeaderLine(lines)
        val startIndex = if (headerIndex >= 0) headerIndex + 1 else 0
        val colMap = if (headerIndex >= 0) mapColumns(lines[headerIndex]) else null

        return lines.drop(startIndex).mapNotNull { line ->
            parseCsvLine(line, colMap)
        }
    }

    private fun findHeaderLine(lines: List<String>): Int {
        val headerKeywords = listOf("日期", "金额", "类型", "分类", "备注", "date", "amount", "time")
        return lines.indexOfFirst { line ->
            val lower = line.lowercase()
            headerKeywords.count { keyword -> lower.contains(keyword) } >= 2
        }
    }

    private fun mapColumns(header: String): Map<Int, String> {
        val cols = splitCsvLine(header)
        return cols.mapIndexedNotNull { index, name ->
            val key = when {
                name.contains("日期") || name.contains("date") || name.contains("time") -> "date"
                name.contains("金额") || name.contains("amount") || name.contains("money") -> "amount"
                name.contains("类型") || name.contains("type") || name.contains("收支") -> "type"
                name.contains("分类") || name.contains("category") -> "category"
                name.contains("备注") || name.contains("description") || name.contains("desc") || name.contains("说明") -> "description"
                name.contains("支付方式") || name.contains("来源") || name.contains("source") -> "source"
                else -> null
            }
            if (key != null) index to key else null
        }.toMap()
    }

    private fun parseCsvLine(line: String, colMap: Map<Int, String>?): ParsedBill? {
        val cols = splitCsvLine(line)

        if (colMap != null) {
            var amount: Double? = null
            var type: TransactionType = TransactionType.EXPENSE
            var category = "其他"
            var desc = ""
            var dateMillis = System.currentTimeMillis()

            var source = TransactionSource.MANUAL

            colMap.forEach { (index, key) ->
                val value = cols.getOrNull(index)?.trim() ?: return@forEach
                when (key) {
                    "amount" -> amount = value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                    "type" -> {
                        type = when {
                            value.contains("收") || value.uppercase() == "INCOME" -> TransactionType.INCOME
                            else -> TransactionType.EXPENSE
                        }
                    }
                    "category" -> category = value.ifBlank { "其他" }
                    "description" -> desc = value
                    "source" -> source = detectPaymentSource(value)
                    "date" -> {
                        val parsed = tryParseDate(value)
                        if (parsed != null) dateMillis = parsed
                    }
                }
            }

            // Fallback: detect payment source from description and category
            if (source == TransactionSource.MANUAL) {
                source = detectPaymentSource("$desc $category")
            }

            val amt = amount ?: return null
            return ParsedBill(dateMillis, amt, type, category, desc, source)
        }

        // Fallback: positional parsing (date, amount, type, category, description)
        val amt = cols.getOrNull(1)?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: return null
        val t = cols.getOrNull(2)?.let {
            when {
                it.contains("收") || it.uppercase() == "INCOME" -> TransactionType.INCOME
                else -> TransactionType.EXPENSE
            }
        } ?: TransactionType.EXPENSE
        val cat = cols.getOrNull(3).takeIf { !it.isNullOrBlank() } ?: "其他"
        val desc = cols.getOrNull(4).orEmpty()
        val dateMillis = cols.getOrNull(0)?.let { tryParseDate(it) } ?: System.currentTimeMillis()
        val source = detectPaymentSource("$desc $cat ${cols.joinToString(" ")}")

        return ParsedBill(dateMillis, amt, t, cat, desc, source)
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun tryParseDate(text: String): Long? {
        val clean = text.trim().replace("年", "-").replace("月", "-").replace("日", "")
        for (fmt in dateFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(clean) ?: continue
                // If format doesn't include year, use current year
                if (!fmt.contains("yyyy")) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = date
                    cal.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
                    return cal.timeInMillis
                }
                return date.time
            } catch (_: Exception) { }
        }
        return null
    }

    // OCR text parsing for payment screenshots (Taobao, Pinduoduo, WeChat, Alipay)
    fun parseOcrText(rawText: String): List<ParsedBill> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val fullText = rawText.replace(" ", "").replace("\n", "")
        val bills = mutableListOf<ParsedBill>()

        // Try to extract amount - look for various Chinese payment patterns
        val (amount, isIncome) = extractAmountWithType(fullText) ?: return emptyList()

        // Extract merchant/store name
        val merchant = extractMerchant(lines, fullText)

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val defaultCategory = if (isIncome) "工资" else "购物"

        // Detect payment source from OCR text
        val source = detectPaymentSource(fullText + " " + lines.joinToString(" "))

        // Extract date
        val dateMillis = extractDate(fullText) ?: System.currentTimeMillis()

        bills.add(ParsedBill(dateMillis, amount, type, defaultCategory, merchant, source))
        return bills
    }

    // Returns (amount, isIncome) or null
    private fun extractAmountWithType(text: String): Pair<Double, Boolean>? {
        // Pattern 1: 支出语境 — 负号附近有关键词
        AmountParser.EXPENSE_CONTEXT.find(text)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && AmountParser.valid(v)) return Pair(v, false)
        }

        // Pattern 2: 行首/空格后的独立负号金额
        AmountParser.STANDALONE_MINUS.find(text)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && AmountParser.valid(v)) return Pair(v, false)
        }

        // Pattern 3: 收入语境 — 正号或收入关键词
        AmountParser.INCOME_CONTEXT.find(text)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && AmountParser.valid(v)) return Pair(v, true)
        }

        // Pattern 4: 显式正号
        AmountParser.PLUS_SIGN.find(text)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && AmountParser.valid(v)) return Pair(v, true)
        }

        // 兜底
        val amount = extractAmount(text) ?: return null
        val isIncome = text.contains("收入") || text.contains("收款") || text.contains("转入")
        return Pair(amount, isIncome)
    }

    private fun extractAmount(text: String): Double? {
        // 优先匹配支付关键词
        AmountParser.paymentKeyword(text)?.let { return it }
        // 兜底：取最后一个符号金额（OCR 场景通常是合计）
        return AmountParser.lastSymbolAmount(text)
    }

    private fun extractMerchant(lines: List<String>, fullText: String): String {
        // Look for store/shop indicators
        val merchantPatterns = listOf(
            Regex("收款方[：:\\s]*(.+)"),
            Regex("商家[：:\\s]*(.+)"),
            Regex("商户[：:\\s]*(.+)"),
            Regex("店铺[：:\\s]*(.+)"),
            Regex("(.+(?:小店|旗舰店|专卖店|超市|便利店|商城))"),
        )
        for (pattern in merchantPatterns) {
            val m = pattern.find(fullText) ?: continue
            val name = m.groupValues[1].trim().take(30)
            if (name.isNotBlank()) return name
        }

        // Look for the longest non-empty line that might be a shop name
        // (lines between header and amount area)
        return lines.firstOrNull { it.length in 2..20 && !it.contains(Regex("[\\d¥￥]")) && !it.contains("支付|微信|支付宝|淘宝|拼多多") }
            ?.take(30) ?: ""
    }

    private fun extractDate(text: String): Long? {
        val dateRegex = Regex("(\\d{4})\\D?(\\d{1,2})\\D?(\\d{1,2})")
        val m = dateRegex.find(text) ?: return null
        try {
            val cal = java.util.Calendar.getInstance()
            cal.set(m.groupValues[1].toInt(), m.groupValues[2].toInt() - 1, m.groupValues[3].toInt(), 0, 0, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        } catch (_: Exception) { return null }
    }
}
