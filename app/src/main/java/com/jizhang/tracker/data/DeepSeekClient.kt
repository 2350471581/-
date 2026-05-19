package com.jizhang.tracker.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekClient @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val TAG = "DeepSeekClient"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1000L
    }

    private val apiKey = ApiKeyManager.deepseekApiKey

    /**
     * 发起 DeepSeek API 请求，带重试逻辑。
     */
    suspend fun <T> executeWithRetry(
        body: JSONObject,
        parse: (String) -> T?
    ): Result<T> {
        var lastError: Exception? = null

        for (attempt in 0..MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(AppConfig.DEEPSEEK_API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    val code = response.code
                    Log.w(TAG, "API returned HTTP $code (attempt ${attempt + 1})")
                    when {
                        code == 401 || code == 403 -> {
                            return Result.failure(AIBillException("API Key 无效，请在设置中更新", isAuthError = true))
                        }
                        code == 429 -> {
                            lastError = AIBillException("请求过于频繁，请稍后再试")
                            if (attempt < MAX_RETRIES) {
                                kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                                continue
                            }
                            return Result.failure(lastError)
                        }
                        else -> {
                            lastError = AIBillException("服务暂不可用 (HTTP $code)")
                            if (attempt < MAX_RETRIES) {
                                kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                                continue
                            }
                            return Result.failure(lastError)
                        }
                    }
                }

                if (responseBody == null) {
                    lastError = AIBillException("服务返回空响应")
                    continue
                }

                val json = JSONObject(responseBody)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                val result = parse(content)
                if (result != null) {
                    return Result.success(result)
                }
                lastError = AIBillException("无法解析AI响应")
                if (attempt < MAX_RETRIES) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                    continue
                }
            } catch (e: UnknownHostException) {
                Log.w(TAG, "Network unavailable", e)
                return Result.failure(AIBillException("网络连接不可用，请检查网络", isNetworkError = true))
            } catch (e: java.net.SocketTimeoutException) {
                Log.w(TAG, "Request timed out (attempt ${attempt + 1})", e)
                lastError = AIBillException("请求超时，正在重试...", isNetworkError = true)
            } catch (e: java.io.IOException) {
                Log.w(TAG, "IO error (attempt ${attempt + 1})", e)
                lastError = AIBillException("网络异常: ${e.message?.take(50) ?: "未知错误"}", isNetworkError = true)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                return Result.failure(AIBillException("未知错误: ${e.message?.take(80) ?: ""}"))
            }
        }

        return Result.failure(lastError ?: AIBillException("请求失败"))
    }

    /** 从 AI 响应中提取 JSON 内容（去除 markdown 代码块标记） */
    fun extractJson(content: String): String {
        return if (content.startsWith("```")) {
            content.trimStart('`').substringAfter("json").trim().trimStart('`').trim()
        } else content
    }
}
