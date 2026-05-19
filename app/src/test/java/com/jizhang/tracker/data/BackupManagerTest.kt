package com.jizhang.tracker.data

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupManagerTest {

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any<Throwable>()) } returns 0
    }

    @Test
    fun `backup model serialization round-trip`() {
        val adapter = com.squareup.moshi.Moshi.Builder().build()
            .adapter(BackupData::class.java)

        val backup = BackupData(
            version = "0.8",
            exportDate = 1000L,
            transactions = listOf(
                BackupTransaction(100L, 50.0, "EXPENSE", "MANUAL", "餐饮", "午餐")
            ),
            plans = BackupPlans(balance = 1000.0),
            settings = BackupSettings(nickname = "测试"),
            checksum = null
        )

        val json = adapter.toJson(backup)
        assertTrue(json.contains("50.0"))
        assertTrue(json.contains("午餐"))

        val parsed = adapter.fromJson(json)!!
        assertEquals("0.8", parsed.version)
        assertEquals(1, parsed.transactions.size)
        assertEquals(50.0, parsed.transactions[0].amount, 0.001)
    }

    @Test
    fun `backup with checksum serialization`() {
        val adapter = com.squareup.moshi.Moshi.Builder().build()
            .adapter(BackupData::class.java)

        // 先序列化无 checksum 版本
        val backup = BackupData(
            version = "0.8",
            exportDate = 2000L,
            transactions = listOf(
                BackupTransaction(200L, 30.0, "INCOME", "WECHAT", "工资", "工资到账")
            ),
            plans = null,
            settings = null,
            checksum = null
        )
        val jsonNoChecksum = adapter.toJson(backup)

        // 计算 SHA-256
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(jsonNoChecksum.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }

        // 带 checksum 版
        val signed = backup.copy(checksum = hex)
        val jsonWithChecksum = adapter.toJson(signed)
        assertTrue(jsonWithChecksum.contains(hex))
        assertTrue(jsonWithChecksum.contains(hex.take(8))) // checksum 确实写入了

        // 校验流程：去掉 checksum 重新算
        val parsed = adapter.fromJson(jsonWithChecksum)!!
        assertEquals(hex, parsed.checksum)

        val reParsed = parsed.copy(checksum = null)
        val reJson = adapter.toJson(reParsed)
        val reDigest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(reJson.toByteArray())
        val reHex = reDigest.joinToString("") { "%02x".format(it) }
        assertEquals(hex, reHex)
    }

    @Test
    fun `tampered data fails checksum verification`() {
        val adapter = com.squareup.moshi.Moshi.Builder().build()
            .adapter(BackupData::class.java)

        val backup = BackupData(
            version = "0.8",
            exportDate = 3000L,
            transactions = listOf(
                BackupTransaction(300L, 100.0, "EXPENSE", null, "购物", "买衣服")
            ),
            checksum = null
        )
        val jsonNoChecksum = adapter.toJson(backup)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(jsonNoChecksum.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }

        val signed = backup.copy(checksum = hex)
        var jsonWithChecksum = adapter.toJson(signed)

        // 篡改金额
        val tampered = jsonWithChecksum.replace("100.0", "9999.0")
        val parsed = adapter.fromJson(tampered)!!

        // 校验应失败
        val unsigned = parsed.copy(checksum = null)
        val verifyJson = adapter.toJson(unsigned)
        val verifyDigest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(verifyJson.toByteArray())
        val verifyHex = verifyDigest.joinToString("") { "%02x".format(it) }
        assertNotEquals(verifyHex, parsed.checksum)
    }

    @Test
    fun `empty backup round-trip`() {
        val adapter = com.squareup.moshi.Moshi.Builder().build()
            .adapter(BackupData::class.java)

        val backup = BackupData(
            version = "0.8",
            exportDate = System.currentTimeMillis(),
            transactions = emptyList()
        )

        val json = adapter.toJson(backup)
        val parsed = adapter.fromJson(json)!!
        assertTrue(parsed.transactions.isEmpty())
    }

    @Test
    fun `multiple transactions round-trip`() {
        val adapter = com.squareup.moshi.Moshi.Builder().build()
            .adapter(BackupData::class.java)

        val backup = BackupData(
            version = "0.8",
            exportDate = 4000L,
            transactions = (1..10).map { i ->
                BackupTransaction(
                    dateMillis = 1000L * i,
                    amount = i * 10.0,
                    type = if (i % 2 == 0) "INCOME" else "EXPENSE",
                    source = if (i % 3 == 0) "WECHAT" else "ALIPAY",
                    category = "分类$i",
                    description = "记录$i"
                )
            }
        )

        val json = adapter.toJson(backup)
        val parsed = adapter.fromJson(json)!!
        assertEquals(10, parsed.transactions.size)
        assertEquals(100.0, parsed.transactions[9].amount, 0.001)
        assertEquals("记录5", parsed.transactions[4].description)
    }
}
