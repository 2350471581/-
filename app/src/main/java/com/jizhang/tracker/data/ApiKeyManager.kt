package com.jizhang.tracker.data

import java.util.Base64

object ApiKeyManager {
    val deepseekApiKey: String by lazy { decodeDefaultKey() }

    private val DEFAULT_KEY_SEGMENTS = listOf(
        "c2stODAx", "NGE3MTIy", "NWE2NDlj", "MzhhMmEx", "YmYyOGUz",
        "MTRiMDU="
    )

    private fun decodeDefaultKey(): String {
        val raw = DEFAULT_KEY_SEGMENTS.joinToString("")
        return String(Base64.getDecoder().decode(raw))
    }
}
