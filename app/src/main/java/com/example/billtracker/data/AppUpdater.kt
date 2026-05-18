package com.example.billtracker.data

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateSource(
    val url: String,
    val priority: Int,
    val label: String = "源 $priority",
    var latencyMs: Long = -1
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val md5: String,
    val sources: List<UpdateSource>,
    val lanzouUrl: String = "",
    val lanzouPassword: String = ""
)

sealed class UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult()
    data object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}

object AppUpdater {

    private const val CHECK_URL = "https://api.github.com/repos/2350471581/-/contents/version.json"
    private val FALLBACK_CHECK_URLS = listOf(
        "https://raw.githubusercontent.com/2350471581/-/main/version.json",
        "https://ghproxy.com/https://raw.githubusercontent.com/2350471581/-/main/version.json",
        "https://gitee.com/doting-love/billing-assistant/raw/master/version.json"
    )
    private const val PREFS_NAME = "app_updater"
    private const val KEY_LAST_SOURCE = "last_source_url"

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (_: Exception) { 1 }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }
    }

    /** 测试单个源的响应时间（毫秒） */
    private fun measureLatency(urlStr: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 3000
            conn.instanceFollowRedirects = false
            conn.requestMethod = "HEAD"
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..399) System.currentTimeMillis() - start else -1
        } catch (_: Exception) { -1 }
    }

    /** 多源失败重试检查更新，并对下载源测速排序 */
    suspend fun checkUpdate(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        val urls = listOf(CHECK_URL) + FALLBACK_CHECK_URLS

        for (urlStr in urls) {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"

                if (conn.responseCode != 200) continue

                val body = conn.inputStream.bufferedReader().readText()
                val info = parseVersionJson(body)
                if (info != null) {
                    val current = getCurrentVersionCode(context)

                    // 对下载源测速，按速度排序
                    val tested = info.sources.map { source ->
                        source.copy(latencyMs = measureLatency(source.url))
                    }.sortedWith(compareBy<UpdateSource> { it.latencyMs }.thenBy { it.priority })

                    val sortedInfo = info.copy(sources = tested)

                    return@withContext if (sortedInfo.versionCode > current) {
                        UpdateResult.Available(sortedInfo)
                    } else {
                        UpdateResult.UpToDate
                    }
                }
            } catch (_: Exception) { }
        }
        UpdateResult.Error("无法连接到更新服务器")
    }

    private fun parseVersionJson(raw: String): UpdateInfo? {
        return try {
            val json = org.json.JSONObject(raw)
            // GitHub API content wrapper → 解 base64
            val actual = if (json.has("content")) {
                val b64 = json.getString("content")
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                org.json.JSONObject(String(bytes, Charsets.UTF_8))
            } else json

            val sources = mutableListOf<UpdateSource>()
            if (actual.has("sources")) {
                val arr = actual.getJSONArray("sources")
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    sources += UpdateSource(
                        url = s.getString("url"),
                        priority = s.optInt("priority", i),
                        label = s.optString("label", "源 ${i + 1}")
                    )
                }
                sources.sortBy { it.priority }
            }
            // 兼容旧版单 URL
            if (sources.isEmpty() && actual.has("apkUrl")) {
                sources += UpdateSource(actual.getString("apkUrl"), 0, "主站")
            }

            UpdateInfo(
                versionCode = actual.getInt("versionCode"),
                versionName = actual.getString("versionName"),
                releaseNotes = actual.optString("releaseNotes", ""),
                md5 = actual.optString("md5", ""),
                sources = sources,
                lanzouUrl = actual.optString("lanzouUrl", ""),
                lanzouPassword = actual.optString("lanzouPassword", "")
            )
        } catch (_: Exception) { null }
    }

    /**
     * 多源下载 + 断点续传 + MD5 校验
     *
     * 1. 如果本地已有完整 APK 且 MD5 匹配 → 直接返回
     * 2. 按优先级排序源，上次成功的源优先，可用的源优先
     * 3. 逐个尝试，失败自动切下一个源
     * 4. 支持 HTTP Range 续传（.tmp 文件）
     * 5. 下载完校验 MD5，不匹配则删掉重试下一个源
     */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fileName = "billtracker-v${info.versionName}.apk"
        val file = File(context.cacheDir, fileName)

        // 已有完整文件 → MD5 快速校验
        if (file.exists() && info.md5.isNotEmpty()) {
            if (verifyMd5(file, info.md5)) {
                onProgress(100)
                return@withContext DownloadResult.Success(file)
            }
            file.delete()
        }

        // 过滤蓝奏云（非直链），只保留直链下载源
        val directSources = info.sources.filter { !it.url.contains("lanzou") }

        // 可用的源（latency != -1）排前面，不可用的排最后，同可用性按 priority
        val sorted = directSources.sortedWith(
            compareBy<UpdateSource> { if (it.latencyMs >= 0) 0 else 1 }
                .thenBy { it.priority }
        )
        if (sorted.isEmpty()) {
            return@withContext DownloadResult.Error("没有可用的下载源")
        }

        val lastUrl = prefs.getString(KEY_LAST_SOURCE, null)
        val ordered = if (lastUrl != null) {
            val idx = sorted.indexOfFirst { it.url == lastUrl }
            if (idx > 0) listOf(sorted[idx]) + sorted.filterIndexed { i, _ -> i != idx }
            else sorted
        } else sorted

        for (source in ordered) {
            when (val r = downloadFromSource(context, source, info.md5, fileName, onProgress)) {
                is DownloadResult.Success -> {
                    prefs.edit().putString(KEY_LAST_SOURCE, source.url).apply()
                    return@withContext r
                }
                is DownloadResult.Error -> { }
            }
        }
        DownloadResult.Error("所有下载源均失败")
    }

    private suspend fun downloadFromSource(
        context: Context,
        source: UpdateSource,
        expectedMd5: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        val tmpFile = File(context.cacheDir, "${fileName}.tmp")
        val finalFile = File(context.cacheDir, fileName)

        try {
            val url = URL(source.url)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000

            var downloaded = 0L
            if (tmpFile.exists()) {
                downloaded = tmpFile.length()
                if (downloaded > 0) conn.setRequestProperty("Range", "bytes=$downloaded-")
            }

            conn.connect()
            val code = conn.responseCode
            if (code != 200 && code != 206) {
                return@withContext DownloadResult.Error("${source.label} 返回 $code")
            }

            val totalLen = if (code == 206) {
                val cr = conn.getHeaderField("Content-Range") ?: ""
                """/(\d+)""".toRegex().find(cr)?.groupValues?.get(1)?.toLongOrNull()
                    ?: (conn.contentLengthLong + downloaded)
            } else conn.contentLengthLong

            val raf = RandomAccessFile(tmpFile, "rw")
            if (downloaded > 0) raf.seek(downloaded)

            conn.inputStream.use { input ->
                val buf = ByteArray(8192)
                var lastPct = -1
                var total = downloaded
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    raf.write(buf, 0, read)
                    total += read
                    if (totalLen > 0) {
                        val pct = (total * 100 / totalLen).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            withContext(Dispatchers.Main) { onProgress(pct) }
                        }
                    }
                }
            }
            raf.close()

            // MD5 校验
            if (expectedMd5.isNotEmpty() && !verifyMd5(tmpFile, expectedMd5)) {
                tmpFile.delete()
                return@withContext DownloadResult.Error("${source.label} MD5 校验失败")
            }

            tmpFile.renameTo(finalFile)
            onProgress(100)
            DownloadResult.Success(finalFile)

        } catch (e: Exception) {
            DownloadResult.Error("${source.label}: ${e.message}")
        }
    }

    private fun verifyMd5(file: File, expected: String): Boolean {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                var read: Int
                while (input.read(buf).also { read = it } != -1) md.update(buf, 0, read)
            }
            val actual = md.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expected, ignoreCase = true)
        } catch (_: Exception) { false }
    }

    fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "安装失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
