package com.jizhang.tracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {

    private val aMinuteAgo = System.currentTimeMillis() - 60000

    private fun notif(title: String, text: String) = NotificationMessage(
        tag = "test", id = 1, title = title, text = text, dateMillis = aMinuteAgo
    )

    @Test
    fun `wechat payment notification`() {
        val result = NotificationParser.parse(
            notif("微信支付", "¥35.00 美宜佳"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(35.0, result!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(TransactionSource.WECHAT, result.source)
    }

    @Test
    fun `alipay receipt notification`() {
        val result = NotificationParser.parse(
            notif("支付宝", "到账¥100.00"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(TransactionSource.ALIPAY, result.source)
    }

    @Test
    fun `custom trigger keyword notification`() {
        val result = NotificationParser.parse(
            notif("美团", "消费¥50.00"), setOf("微信", "支付宝", "美团")
        )
        val r = requireNotNull(result)
        assertEquals(50.0, r.amount, 0.001)
        assertEquals(TransactionSource.UNKNOWN, r.source) // 非微信非支付宝的第三方通知
    }

    @Test
    fun `refund is income`() {
        val result = NotificationParser.parse(
            notif("支付宝", "退款¥200.00已到账"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
    }

    @Test
    fun `xiangni shoukuan is expense for wechat`() {
        val result = NotificationParser.parse(
            notif("微信", "向你收款¥300.00"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
    }

    @Test
    fun `xiangni fukuan is income for wechat`() {
        val result = NotificationParser.parse(
            notif("微信", "向你付款¥500.00"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result!!.type)
    }

    @Test
    fun `notification without trigger returns null`() {
        assertNull(NotificationParser.parse(
            notif("天气", "今天晴朗"), setOf("微信", "支付宝")
        ))
    }

    @Test
    fun `negative amount detected as expense`() {
        val result = NotificationParser.parse(
            notif("支付宝", "消费-¥150.00"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result!!.type)
    }

    @Test
    fun `category detection works`() {
        val result = NotificationParser.parse(
            notif("微信支付", "午餐¥35.00"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(TransactionSource.WECHAT, result!!.source)
    }

    @Test
    fun `title trigger detection works`() {
        val result = NotificationParser.parse(
            notif("微信支付", "¥35.00"), setOf("微信", "支付宝")
        )
        assertNotNull(result)
        assertEquals(35.0, result!!.amount, 0.001)
    }
}
