package com.example.billtracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.example.billtracker.ui.BillTrackerTheme
import com.example.billtracker.ui.MainScreen
import com.example.billtracker.ui.BLUE_PINK_THEME_INDEX

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)
        setContent {
            var themeIndex by remember { mutableStateOf(prefs.getInt("theme_index", 2)) }
            var followSystemTheme by remember { mutableStateOf(prefs.getBoolean("follow_system_theme", false)) }
            val systemDark = isSystemInDarkTheme()
            val isDark = followSystemTheme && systemDark

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "theme_index" -> themeIndex = prefs.getInt("theme_index", 0)
                        "follow_system_theme" -> followSystemTheme = prefs.getBoolean("follow_system_theme", false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            val effectiveThemeIndex = if (isDark) BLUE_PINK_THEME_INDEX else themeIndex
            BillTrackerTheme(themeIndex = effectiveThemeIndex, isDarkTheme = isDark) {
                MainScreen()
            }
        }
    }
}
