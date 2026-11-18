package com.shadownet.calculator.ui.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * Available theme mode preferences.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Manages persistence and retrieval of the user's theme mode selection using SharedPreferences.
 * Defaults to [ThemeMode.SYSTEM] for new installations.
 */
object ThemePreferenceManager {
    private const val PREFS_NAME = "calculator_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    fun getThemeMode(context: Context): ThemeMode {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(modeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
