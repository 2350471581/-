package com.example.billtracker.data

import android.content.Context
import android.content.SharedPreferences

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

class PlanStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)

    var balance: Double
        get() = prefs.getFloat("plan_balance", 0f).toDouble()
        set(value) = prefs.edit().putFloat("plan_balance", value.toFloat()).apply()

    var todayPlanTarget: Double
        get() = prefs.getFloat("today_plan_target", 0f).toDouble()
        set(value) = prefs.edit().putFloat("today_plan_target", value.toFloat()).apply()

    var totalPlanTarget: Double
        get() = prefs.getFloat("total_plan_target", 0f).toDouble()
        set(value) = prefs.edit().putFloat("total_plan_target", value.toFloat()).apply()

    var savePlanTarget: Double
        get() = prefs.getFloat("save_plan_target", 0f).toDouble()
        set(value) = prefs.edit().putFloat("save_plan_target", value.toFloat()).apply()

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
        val name = prefs.getString("custom_plan_name_$index", null) ?: return null
        val target = prefs.getFloat("custom_plan_target_$index", 0f).toDouble()
        val note = prefs.getString("custom_plan_note_$index", "") ?: ""
        val typeName = prefs.getString("custom_plan_type_$index", "TODAY_NET") ?: "TODAY_NET"
        val type = try { PlanDataType.valueOf(typeName) } catch (_: Exception) { PlanDataType.TODAY_NET }
        return CustomPlan(name, target, note, type)
    }

    fun getAllCustomPlans(): List<CustomPlan> {
        val count = customPlanCount
        return (0 until count).mapNotNull { getCustomPlan(it) }
    }

    fun addCustomPlan(plan: CustomPlan) {
        val index = customPlanCount
        prefs.edit()
            .putString("custom_plan_name_$index", plan.name)
            .putFloat("custom_plan_target_$index", plan.target.toFloat())
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
        get() = prefs.getInt("theme_index", 0)
        set(value) = prefs.edit().putInt("theme_index", value).apply()

    // ── 跟随系统主题 ──
    var followSystemTheme: Boolean
        get() = prefs.getBoolean("follow_system_theme", false)
        set(value) = prefs.edit().putBoolean("follow_system_theme", value).apply()

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

    fun updateCustomPlan(index: Int, target: Double, note: String) {
        val name = prefs.getString("custom_plan_name_$index", null) ?: return
        prefs.edit()
            .putFloat("custom_plan_target_$index", target.toFloat())
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
                .putFloat("custom_plan_target_$i", next.target.toFloat())
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
