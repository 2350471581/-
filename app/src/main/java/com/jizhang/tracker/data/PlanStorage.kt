package com.jizhang.tracker.data

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

enum class PlanDataType {
    TODAY_INCOME, TODAY_EXPENSE, TODAY_NET,
    TOTAL_INCOME, TOTAL_EXPENSE, TOTAL_NET
}

fun PlanDataType.displayName(): String = when (this) {
    PlanDataType.TODAY_INCOME -> "今日收入"
    PlanDataType.TODAY_EXPENSE -> "今日支出"
    PlanDataType.TODAY_NET -> "今日净收入"
    PlanDataType.TOTAL_INCOME -> "总收入"
    PlanDataType.TOTAL_EXPENSE -> "总支出"
    PlanDataType.TOTAL_NET -> "总净收入"
}

data class CustomPlan(
    val name: String,
    val target: Double = 0.0,
    val note: String = "",
    val type: PlanDataType = PlanDataType.TODAY_NET
)

@Singleton
class PlanStorage @Inject constructor(private val prefs: SharedPreferences) {

    var balance: Double
        get() = prefs.getLong("plan_balance", 0L) / 100.0
        set(value) = prefs.edit().putLong("plan_balance", (value * 100).toLong()).apply()

    var todayPlanTarget: Double
        get() = prefs.getLong("today_plan_target", 0L) / 100.0
        set(value) = prefs.edit().putLong("today_plan_target", (value * 100).toLong()).apply()

    var totalPlanTarget: Double
        get() = prefs.getLong("total_plan_target", 0L) / 100.0
        set(value) = prefs.edit().putLong("total_plan_target", (value * 100).toLong()).apply()

    var savePlanTarget: Double
        get() = prefs.getLong("save_plan_target", 0L) / 100.0
        set(value) = prefs.edit().putLong("save_plan_target", (value * 100).toLong()).apply()

    var todayPlanNote: String
        get() = prefs.getString("today_plan_note", "") ?: ""
        set(value) = prefs.edit().putString("today_plan_note", value).apply()

    var totalPlanNote: String
        get() = prefs.getString("total_plan_note", "") ?: ""
        set(value) = prefs.edit().putString("total_plan_note", value).apply()

    var savePlanNote: String
        get() = prefs.getString("save_plan_note", "") ?: ""
        set(value) = prefs.edit().putString("save_plan_note", value).apply()

    // ── 自定义计划 ──
    private var customPlanCount: Int
        get() = prefs.getInt("custom_plan_count", 0)
        set(value) = prefs.edit().putInt("custom_plan_count", value).apply()

    fun getCustomPlan(index: Int): CustomPlan? {
        val pname = prefs.getString("custom_plan_name_$index", null) ?: return null
        val target = prefs.getLong("custom_plan_target_$index", 0L) / 100.0
        val note = prefs.getString("custom_plan_note_$index", "") ?: ""
        val typeName = prefs.getString("custom_plan_type_$index", "TODAY_NET") ?: "TODAY_NET"
        val type = try { PlanDataType.valueOf(typeName) } catch (_: Exception) { PlanDataType.TODAY_NET }
        return CustomPlan(pname, target, note, type)
    }

    fun getAllCustomPlans(): List<CustomPlan> {
        val count = customPlanCount
        return (0 until count).mapNotNull { getCustomPlan(it) }
    }

    fun addCustomPlan(plan: CustomPlan) {
        val index = customPlanCount
        prefs.edit()
            .putString("custom_plan_name_$index", plan.name)
            .putLong("custom_plan_target_$index", (plan.target * 100).toLong())
            .putString("custom_plan_note_$index", plan.note)
            .putString("custom_plan_type_$index", plan.type.name)
            .putInt("custom_plan_count", index + 1)
            .apply()
    }

    // ── 安装日期 ──
    val installDateMillis: Long
        get() {
            val stored = prefs.getLong("install_date", -1L)
            if (stored == -1L) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong("install_date", now).apply()
                return now
            }
            return stored
        }

    // ── 首次启动 ──
    val isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch_pending", true)

    fun markFirstLaunchSeen() {
        prefs.edit().putBoolean("first_launch_pending", false).apply()
    }

    // ── 手动/自动模式 ──
    var isManualMode: Boolean
        get() = prefs.getBoolean("manual_mode", true) // 默认手动
        set(value) = prefs.edit().putBoolean("manual_mode", value).apply()

    // ── 主题 ──
    var themeIndex: Int
        get() = prefs.getInt("theme_index", 2) // 默认鸢尾蓝
        set(value) = prefs.edit().putInt("theme_index", value).apply()

// ── 昵称 ──
    var nickname: String
        get() = prefs.getString("nickname", "用户") ?: "用户"
        set(value) = prefs.edit().putString("nickname", value).apply()

    // ── 头像 emoji 索引 ──
    var avatarEmoji: Int
        get() = prefs.getInt("avatar_emoji", 0)
        set(value) = prefs.edit().putInt("avatar_emoji", value).apply()

    // ── 自定义头像 URI ──
    var customAvatarUri: String
        get() = prefs.getString("custom_avatar_uri", "") ?: ""
        set(value) = prefs.edit().putString("custom_avatar_uri", value).apply()

    // ── AI 聊天式记账 ──
    var aiChatEnabled: Boolean
        get() = prefs.getBoolean("ai_chat_enabled", false)
        set(value) = prefs.edit().putBoolean("ai_chat_enabled", value).apply()

    // ── 自定义主题配置 ──
    var customThemeConfigJson: String
        get() = prefs.getString("custom_theme_config", "") ?: ""
        set(value) = prefs.edit().putString("custom_theme_config", value).apply()

    // ── AI 聊天教程已展示 ──
    var aiChatTutorialDone: Boolean
        get() = prefs.getBoolean("ai_chat_tutorial_done", false)
        set(value) = prefs.edit().putBoolean("ai_chat_tutorial_done", value).apply()

    // ── AI 聊天自定义系统提示词 ──
    var customAiPrompt: String
        get() = prefs.getString("custom_ai_prompt", "") ?: ""
        set(value) = prefs.edit().putString("custom_ai_prompt", value).apply()

    // ── AI 昵称 ──
    var aiNickname: String
        get() = prefs.getString("ai_nickname", "AI 记账助手") ?: "AI 记账助手"
        set(value) = prefs.edit().putString("ai_nickname", value).apply()

    // ── AI 头像 Emoji 索引 ──
    var aiAvatarEmoji: Int
        get() = prefs.getInt("ai_avatar_emoji", 21) // default 🤖
        set(value) = prefs.edit().putInt("ai_avatar_emoji", value).apply()

    // ── AI 自定义头像 URI ──
    var aiCustomAvatarUri: String
        get() = prefs.getString("ai_custom_avatar_uri", "") ?: ""
        set(value) = prefs.edit().putString("ai_custom_avatar_uri", value).apply()

    // ── 搜索历史 ──
    var searchHistory: List<String>
        get() {
            val json = prefs.getString("search_history", null) ?: return emptyList()
            return try {
                val arr = org.json.JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) { emptyList() }
        }
        set(value) {
            val arr = org.json.JSONArray(value.take(20))
            prefs.edit().putString("search_history", arr.toString()).apply()
        }

    // ── 自定义触发关键词 ──
    var triggerKeywords: Set<String>
        get() = prefs.getStringSet("trigger_keywords", setOf("微信", "支付宝"))
            ?: setOf("微信", "支付宝")
        set(value) = prefs.edit().putStringSet("trigger_keywords", value).apply()

    // ── 自定义分类 ──
    var customCategories: List<CustomCategory>
        get() {
            val json = prefs.getString("custom_categories", null) ?: return emptyList()
            return try {
                val arr = org.json.JSONArray(json)
                (0 until arr.length()).map {
                    val obj = arr.getJSONObject(it)
                    CustomCategory(
                        name = obj.getString("name"),
                        icon = obj.optString("icon", "📌"),
                        keywords = obj.optJSONArray("keywords")?.let { arr2 ->
                            (0 until arr2.length()).map { arr2.getString(it) }
                        } ?: emptyList()
                    )
                }
            } catch (_: Exception) { emptyList() }
        }
        set(value) {
            val arr = org.json.JSONArray(value.map { cat ->
                org.json.JSONObject().apply {
                    put("name", cat.name)
                    put("icon", cat.icon)
                    put("keywords", org.json.JSONArray(cat.keywords))
                }
            })
            prefs.edit().putString("custom_categories", arr.toString()).apply()
        }

    fun updateCustomPlan(index: Int, target: Double, note: String) {
        if (!prefs.contains("custom_plan_name_$index")) return
        prefs.edit()
            .putLong("custom_plan_target_$index", (target * 100).toLong())
            .putString("custom_plan_note_$index", note)
            .apply()
    }

    fun deleteCustomPlan(index: Int) {
        val count = customPlanCount
        if (index < 0 || index >= count) return
        // 将后面的计划往前移
        for (i in index until count - 1) {
            val next = getCustomPlan(i + 1) ?: break
            prefs.edit()
                .putString("custom_plan_name_$i", next.name)
                .putLong("custom_plan_target_$i", (next.target * 100).toLong())
                .putString("custom_plan_note_$i", next.note)
                .putString("custom_plan_type_$i", next.type.name)
                .apply()
        }
        val last = count - 1
        prefs.edit()
            .remove("custom_plan_name_$last")
            .remove("custom_plan_target_$last")
            .remove("custom_plan_note_$last")
            .remove("custom_plan_type_$last")
            .putInt("custom_plan_count", count - 1)
            .apply()
    }
}

data class CustomCategory(
    val name: String,
    val icon: String = "📌",
    val keywords: List<String> = emptyList()
)
