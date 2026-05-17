package com.example.billtracker.data

import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedBill(
    val dateMillis: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "其他",
    val description: String = ""
)

object BillImporter {

    private val dateFormats = listOf(
        "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
        "MM-dd", "MM/dd",
        "yyyy年MM月dd日", "MM月dd日"
    )

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
                    "date" -> {
                        val parsed = tryParseDate(value)
                        if (parsed != null) dateMillis = parsed
                    }
                }
            }

            val amt = amount ?: return null
            return ParsedBill(dateMillis, amt, type, category, desc)
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

        return ParsedBill(dateMillis, amt, t, cat, desc)
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
        val amount = extractAmount(fullText) ?: return emptyList()

        // Extract merchant/store name
        val merchant = extractMerchant(lines, fullText)

        // Default to expense for payment screenshots
        val type = TransactionType.EXPENSE

        // Extract date
        val dateMillis = extractDate(fullText) ?: System.currentTimeMillis()

        bills.add(ParsedBill(dateMillis, amount, type, "购物", merchant))
        return bills
    }

    private fun extractAmount(text: String): Double? {
        // Priority order: look for payment amount patterns first

        // Pattern: 实付 ¥XX.XX, 实付￥XX.XX, 合计: XX.XX
        val paymentPatterns = listOf(
            Regex("实付[：:\\s]*[¥￥]?\\s*(\\d+\\.?\\d*)"),
            Regex("合计[：:\\s]*[¥￥]?\\s*(\\d+\\.?\\d*)"),
            Regex("总计[：:\\s]*[¥￥]?\\s*(\\d+\\.?\\d*)"),
            Regex("金额[：:\\s]*[¥￥]?\\s*(\\d+\\.?\\d*)"),
            Regex("收款[：:\\s]*[¥￥]?\\s*(\\d+\\.?\\d*)"),
            Regex("小计[：:\\s]*[¥￥]?\\s*(\\d+\\.?\\d*)"),
        )
        for (pattern in paymentPatterns) {
            val m = pattern.find(text)
            if (m != null) {
                val v = m.groupValues[1].toDoubleOrNull()
                if (v != null && v in 0.01..999999.0) return v
            }
        }

        // Pattern: ¥XX.XX or ￥XX.XX at the start or standalone
        val symbolPatterns = listOf(
            Regex("""[¥￥]\s*(\d+\.?\d*)"""),
            Regex("""(\d+\.?\d*)\s*元"""),
        )
        for (pattern in symbolPatterns) {
            val matches = pattern.findAll(text).toList()
            if (matches.isNotEmpty()) {
                // Take the last (largest / most likely total) amount
                val best = matches.lastOrNull() ?: matches.first()
                val v = best.groupValues[1].toDoubleOrNull()
                if (v != null && v in 0.01..999999.0) return v
            }
        }

        return null
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
