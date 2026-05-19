package com.jizhang.tracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    private val aMinuteAgo = System.currentTimeMillis() - 60000
    private val defaultKeywords = setOf("微信", "支付宝")

    private fun msg(body: String) = SmsMessage(id = 1, body = body, dateMillis = aMinuteAgo)

    @Test
    fun `wechat expense payment`() {
        val result = SmsParser.parse(msg("微信支付付款35.00元给美宜佳"), defaultKeywords)
        assertNotNull(result)
        assertEquals(35.0, result!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(TransactionSource.WECHAT, result.source)
    }

    @Test
    fun `wechat income receipt`() {
        val result = SmsParser.parse(msg("微信收款100.00元"), defaultKeywords)
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(TransactionSource.WECHAT, result.source)
    }

    @Test
    fun `wechat refund`() {
        val result = SmsParser.parse(msg("微信退款50.00元已到账"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertEquals(TransactionSource.WECHAT, result.source)
    }

    @Test
    fun `alipay expense`() {
        val result = SmsParser.parse(msg("支付宝支出¥128.00"), defaultKeywords)
        assertNotNull(result)
        assertEquals(128.0, result!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(TransactionSource.ALIPAY, result.source)
    }

    @Test
    fun `alipay income`() {
        val result = SmsParser.parse(msg("支付宝到账500.00元"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertEquals(TransactionSource.ALIPAY, result.source)
    }

    @Test
    fun `bank sms expense`() {
        val result = SmsParser.parse(msg("工商银行支出1000.00元"), defaultKeywords)
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(TransactionSource.BANK, result.source)
    }

    @Test
    fun `bank sms income`() {
        val result = SmsParser.parse(msg("建设银行到账8000.00元"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
        assertEquals(TransactionSource.BANK, result.source)
    }

    @Test
    fun `irrelevant sms returns null`() {
        assertNull(SmsParser.parse(msg("今天天气不错"), defaultKeywords))
    }

    @Test
    fun `skip wealth management`() {
        assertNull(SmsParser.parse(msg("余额宝到账1.23元"), defaultKeywords))
        assertNull(SmsParser.parse(msg("基金分红0.50元"), defaultKeywords))
        assertNull(SmsParser.parse(msg("理财收益2.00元"), defaultKeywords))
    }

    @Test
    fun `custom trigger keyword`() {
        val result = SmsParser.parse(msg("美团支付35元"), setOf("微信", "支付宝", "美团"))
        assertNotNull(result)
        assertEquals(TransactionSource.MANUAL, result!!.source)
    }

    @Test
    fun `wechat red packet expense`() {
        val result = SmsParser.parse(msg("微信红包发送10.00元"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
    }

    @Test
    fun `wechat red packet income`() {
        val result = SmsParser.parse(msg("微信收到红包1.00元"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
    }

    @Test
    fun `alipay xiangni shoukuan is expense`() {
        val result = SmsParser.parse(msg("支付宝向你收款¥200.00"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
    }

    @Test
    fun `bank with negative amount forces expense`() {
        val result = SmsParser.parse(msg("工商银行人民币-500.00"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
    }

    @Test
    fun `recognizes various bank names`() {
        assertNotNull(SmsParser.parse(msg("招商银行消费¥300.00"), defaultKeywords))
        assertNotNull(SmsParser.parse(msg("农业银行收入¥2000.00"), defaultKeywords))
        assertNotNull(SmsParser.parse(msg("浦发银行扣款¥150.00"), defaultKeywords))
    }

    @Test
    fun `wechat transfer to another is expense`() {
        val result = SmsParser.parse(msg("微信转账¥500.00给朋友"), defaultKeywords)
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
    }
}
