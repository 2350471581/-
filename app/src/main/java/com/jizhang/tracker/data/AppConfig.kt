package com.jizhang.tracker.data

object AppConfig {
    const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

    const val APP_UPDATE_CHECK_URL = "https://raw.githubusercontent.com/2350471581/-/master/version.json"
    val APP_UPDATE_FALLBACK_URLS = emptyList<String>()

    const val APP_LANZOU_URL = "https://wwbnt.lanzoul.com/iayMx3pv51cd"
    const val APP_LANZOU_PASSWORD = "7dcb"
    fun appGithubDownloadUrl(versionName: String) =
        "https://github.com/2350471581/-/releases/download/v$versionName/default.apk"
}
