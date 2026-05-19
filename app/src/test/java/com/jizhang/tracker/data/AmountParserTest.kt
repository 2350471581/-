package com.jizhang.tracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountParserTest {

    @Test
    fun `currency symbol prefix`() {
        assertEquals(100.0, AmountParser.firstValidOf("¥100", AmountParser.CURRENCY_SIMPLE)!!, 0.001)
        assertEquals(99.50, AmountParser.firstValidOf("￥99.50", AmountParser.CURRENCY_SIMPLE)!!, 0.001)
        assertEquals(0.50, AmountParser.firstValidOf("¥0.50", AmountParser.CURRENCY_SIMPLE)!!, 0.001)
    }

    @Test
    fun `yuan suffix`() {
        assertEquals(100.0, AmountParser.firstValidOf("100元", AmountParser.YUAN_SIMPLE)!!, 0.001)
        assertEquals(25.50, AmountParser.firstValidOf("25.50元", AmountParser.YUAN_SIMPLE)!!, 0.001)
        assertEquals(0.10, AmountParser.firstValidOf("0.1元", AmountParser.YUAN_SIMPLE)!!, 0.001)
    }

    @Test
    fun `kuai colloquial`() {
        assertEquals(35.0, AmountParser.firstValidOf("35块", AmountParser.KUAI)!!, 0.001)
        assertEquals(12.5, AmountParser.firstValidOf("12.5块", AmountParser.KUAI)!!, 0.001)
    }

    @Test
    fun `bare number fallback`() {
        assertEquals(50.0, AmountParser.firstValidOf("50", AmountParser.BARE_NUMBER)!!, 0.001)
    }

    @Test
    fun `rmb prefix in bank messages`() {
        assertEquals(200.0, AmountParser.firstValidOf("人民币200.00", AmountParser.RMB_PREFIX)!!, 0.001)
        assertEquals(1500.0, AmountParser.firstValidOf("人民币1500", AmountParser.RMB_PREFIX)!!, 0.001)
    }

    @Test
    fun `currency with commas`() {
        val m = AmountParser.CURRENCY.find("¥1,234.56")
        assertNotNull(m)
        val v = AmountParser.stripCommas(m!!)
        assertNotNull(v)
        assertEquals(1234.56, v!!, 0.001)
    }

    @Test
    fun `negative amount detected`() {
        assertTrue(AmountParser.hasNegative("人民币-100"))
        assertTrue(AmountParser.hasNegative("¥-50"))
    }

    @Test
    fun `valid range bounds`() {
        assertTrue(AmountParser.valid(0.01))
        assertTrue(AmountParser.valid(999999.0))
        assertTrue(AmountParser.valid(100.0))
    }

    @Test
    fun `currency flex handles negative`() {
        val v = AmountParser.currencyFlex("¥-100.00")
        assertNotNull(v)
        assertEquals(100.0, v!!, 0.001)
        val v2 = AmountParser.currencyFlex("￥ -50")
        assertNotNull(v2)
        assertEquals(50.0, v2!!, 0.001)
    }

    @Test
    fun `payment keyword extraction`() {
        assertEquals(35.0, AmountParser.paymentKeyword("实付¥35.00")!!, 0.001)
        assertEquals(128.0, AmountParser.paymentKeyword("合计：¥128")!!, 0.001)
        assertEquals(99.9, AmountParser.paymentKeyword("总计 ￥99.9")!!, 0.001)
    }

    @Test
    fun `currency simple finds first amount`() {
        val v = AmountParser.firstValidOf("¥100 和 ¥200", AmountParser.CURRENCY_SIMPLE)
        assertNotNull(v)
        assertEquals(100.0, v!!, 0.001)
    }

    @Test
    fun `last symbol amount`() {
        val v = AmountParser.lastSymbolAmount("¥100 和 ¥200")
        assertNotNull(v)
        assertEquals(200.0, v!!, 0.001)
    }

    @Test
    fun `no amount returns null`() {
        assertNull(AmountParser.firstValidOf("今天天气不错", AmountParser.CURRENCY_SIMPLE, AmountParser.YUAN_SIMPLE))
    }

    @Test
    fun `daozhang pattern`() {
        assertEquals(5000.0, AmountParser.firstValidOf("到账 5000.00元", AmountParser.DAOZHANG)!!, 0.001)
        assertEquals(300.0, AmountParser.firstValidOf("到账300元", AmountParser.DAOZHANG)!!, 0.001)
    }

    @Test
    fun `expense context pattern`() {
        assertNotNull(AmountParser.EXPENSE_CONTEXT.find("支出 - ¥50.00"))
    }

    @Test
    fun `income context pattern`() {
        assertNotNull(AmountParser.INCOME_CONTEXT.find("收入 + ¥1000.00"))
    }
}
