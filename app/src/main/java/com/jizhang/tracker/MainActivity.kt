package com.jizhang.tracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.jizhang.tracker.ui.BillTrackerTheme
import com.jizhang.tracker.ui.MainScreen
import com.jizhang.tracker.ui.CustomThemeConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)
        setContent {
            var themeIndex by remember { mutableIntStateOf(prefs.getInt("theme_index", 2)) }
            var customThemeJson by remember { mutableStateOf(prefs.getString("custom_theme_config", "") ?: "") }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "theme_index" -> themeIndex = prefs.getInt("theme_index", 0)
                        "custom_theme_config" -> customThemeJson = prefs.getString("custom_theme_config", "") ?: ""
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            val customConfig = remember(customThemeJson) { CustomThemeConfig.fromJson(customThemeJson) }
            BillTrackerTheme(themeIndex = themeIndex, customConfig = customConfig) {
                MainScreen()
            }
        }
    }
}
