package com.jizhang.tracker.data

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DeepSeekClientTest {

    private lateinit var client: OkHttpClient
    private lateinit var deepSeekClient: DeepSeekClient
    private val anyRequest = Request.Builder().url("https://api.deepseek.com/").build()

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any<Throwable>()) } returns 0

        client = mockk(relaxed = true)
        deepSeekClient = DeepSeekClient(client)
    }

    private fun response(
        code: Int = 200,
        body: String = """{"choices":[{"message":{"content":"test response"}}]}"""
    ): Response = Response.Builder()
        .code(code)
        .message(if (code in 200..399) "OK" else "Error")
        .protocol(Protocol.HTTP_1_1)
        .request(anyRequest)
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    private fun aBody() = JSONObject().put("test", true)

    // ── Happy path ──

    @Test
    fun `successful request returns parsed result`() = runBlocking {
        every { client.newCall(any()).execute() } returns response()

        val result = deepSeekClient.executeWithRetry(aBody()) { it.uppercase() }

        assertTrue(result.isSuccess)
        assertEquals("TEST RESPONSE", result.getOrNull())
    }

    @Test
    fun `parse returns null triggers retry then succeeds`() = runBlocking {
        val failResp = response(body = """{"choices":[{"message":{"content":"fail"}}]}""")
        val successResp = response()
        every { client.newCall(any()).execute() } returnsMany listOf(failResp, successResp)

        var callCount = 0
        val result = deepSeekClient.executeWithRetry(aBody()) {
            callCount++
            if (callCount == 1) null else it.uppercase()
        }

        assertTrue(result.isSuccess)
        assertEquals("TEST RESPONSE", result.getOrNull())
        verify(exactly = 2) { client.newCall(any()) }
    }

    // ── Retry scenarios ──

    @Test
    fun `io exception triggers retry then succeeds`() = runBlocking {
        val successResp = response()
        var callCount = 0
        every { client.newCall(any()).execute() } answers {
            callCount++
            if (callCount == 1) throw java.io.IOException("timeout") else successResp
        }

        val result = deepSeekClient.executeWithRetry(aBody()) { it.uppercase() }

        assertTrue(result.isSuccess)
        assertEquals("TEST RESPONSE", result.getOrNull())
        verify(exactly = 2) { client.newCall(any()) }
    }

    @Test
    fun `io exception exhausts retries and returns failure`() = runBlocking {
        every { client.newCall(any()).execute() } throws java.io.IOException("persistent error")

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("网络异常") == true)
        verify(exactly = 3) { client.newCall(any()) }
    }

    @Test
    fun `unsuccessful http response retries then fails`() = runBlocking {
        every { client.newCall(any()).execute() } returns response(code = 500, body = "error")

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("HTTP 500") == true)
        verify(exactly = 3) { client.newCall(any()) }
    }

    // ── Auth errors (no retry) ──

    @Test
    fun `http 401 returns auth error immediately`() = runBlocking {
        every { client.newCall(any()).execute() } returns response(code = 401, body = "unauthorized")

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as? AIBillException
        assertNotNull(ex)
        assertTrue(ex!!.isAuthError)
        verify(exactly = 1) { client.newCall(any()) }
    }

    @Test
    fun `http 403 returns auth error immediately`() = runBlocking {
        every { client.newCall(any()).execute() } returns response(code = 403, body = "forbidden")

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as? AIBillException
        assertNotNull(ex)
        assertTrue(ex!!.isAuthError)
        verify(exactly = 1) { client.newCall(any()) }
    }

    // ── Rate limit ──

    @Test
    fun `http 429 retries then returns failure`() = runBlocking {
        every { client.newCall(any()).execute() } returns response(code = 429, body = "rate limited")

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("频繁") == true)
        verify(exactly = 3) { client.newCall(any()) }
    }

    // ── Network unavailable (immediate) ──

    @Test
    fun `unknown host exception returns network error immediately`() = runBlocking {
        every { client.newCall(any()).execute() } throws UnknownHostException()

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as? AIBillException
        assertNotNull(ex)
        assertTrue(ex!!.isNetworkError)
        assertTrue(ex.message?.contains("网络连接不可用") == true)
        verify(exactly = 1) { client.newCall(any()) }
    }

    // ── Socket timeout ──

    @Test
    fun `socket timeout retries then returns failure`() = runBlocking {
        every { client.newCall(any()).execute() } throws SocketTimeoutException()

        val result = deepSeekClient.executeWithRetry(aBody()) { it }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("超时") == true)
        verify(exactly = 3) { client.newCall(any()) }
    }

    // ── extractJson ──

    @Test
    fun `extractJson strips markdown code block`() {
        val input = "```json\n{\"key\": \"value\"}\n```"
        assertEquals("{\"key\": \"value\"}\n```", deepSeekClient.extractJson(input))
    }

    @Test
    fun `extractJson returns plain json unchanged`() {
        val raw = "{\"key\": \"value\"}"
        assertEquals(raw, deepSeekClient.extractJson(raw))
    }

    @Test
    fun `extractJson handles no json label`() {
        val input = "```\nplain text\n```"
        assertEquals("plain text\n```", deepSeekClient.extractJson(input))
    }

    // ── Null body ──

    @Test
    fun `null response body triggers retry`() = runBlocking {
        val nullBodyResp = Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(anyRequest)
            .build() // no body
        val successResp = response()
        every { client.newCall(any()).execute() } returnsMany listOf(nullBodyResp, successResp)

        val result = deepSeekClient.executeWithRetry(aBody()) { it.uppercase() }

        assertTrue(result.isSuccess)
        assertEquals("TEST RESPONSE", result.getOrNull())
    }
}
