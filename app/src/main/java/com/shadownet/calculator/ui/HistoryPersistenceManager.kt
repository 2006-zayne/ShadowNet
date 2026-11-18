package com.shadownet.calculator.ui

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent storage of calculation history using SharedPreferences.
 */
object HistoryPersistenceManager {
    private const val PREFS_NAME = "calculator_persistent_history_prefs"
    private const val KEY_HISTORY_JSON = "key_history_json"

    fun saveHistory(context: Context, history: List<CalculationHistoryItem>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in history) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("expression", item.expression)
                put("result", item.result)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
    }

    fun loadHistory(context: Context): List<CalculationHistoryItem> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<CalculationHistoryItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CalculationHistoryItem(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        expression = obj.optString("expression", ""),
                        result = obj.optString("result", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
