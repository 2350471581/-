package com.example.billtracker.data

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
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

    // 从 GitHub API 读取 version.json（无需令牌，返回 base64）
    private const val CHECK_URL = "https://api.github.com/repos/2350471581/-/contents/version.json"

    // 获取当前版本号
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (_: Exception) { 1 }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }
    }

    // 检查更新（GitHub API 返回 base64）
    suspend fun checkUpdate(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(CHECK_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"

            val code = conn.responseCode
            if (code != 200) {
                return@withContext UpdateResult.Error("服务器返回 $code")
            }

            val respStr = conn.inputStream.bufferedReader().readText()
            val respJson = org.json.JSONObject(respStr)
            val contentBase64 = respJson.getString("content")
            val jsonBytes = android.util.Base64.decode(contentBase64, android.util.Base64.DEFAULT)
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            val json = org.json.JSONObject(jsonStr)

            val info = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                releaseNotes = json.optString("releaseNotes", "")
            )

            val currentCode = getCurrentVersionCode(context)
            if (info.versionCode > currentCode) {
                UpdateResult.Available(info)
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "网络错误")
        }
    }

    // 下载 APK（带进度回调）
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(info.apkUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.connect()

            val totalLen = conn.contentLength
            val fileName = "billtracker-v${info.versionName}.apk"
            val file = File(context.cacheDir, fileName)

            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0
                    var lastReported = -1
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalLen > 0) {
                            val pct = (downloaded * 100 / totalLen)
                            if (pct != lastReported) {
                                lastReported = pct
                                withContext(Dispatchers.Main) { onProgress(pct) }
                            }
                        }
                    }
                }
            }

            DownloadResult.Success(file)
        } catch (e: Exception) {
            DownloadResult.Error(e.message ?: "下载失败")
        }
    }

    // 安装 APK
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
