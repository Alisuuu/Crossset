package com.alisu.crosssset

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val key: String,
    val oldValue: String,
    val newValue: String,
    val table: SettingsTable,
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {

    private const val PREFS_NAME = "history_prefs"
    private const val HISTORY_KEY = "changes_history"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addChange(context: Context, item: HistoryItem) {
        val history = getHistory(context).toMutableList()
        // If the same key exists in the same table, we can either keep all or update.
        // Let's keep a full record for "Undo" purposes.
        history.add(0, item) // Add to the beginning (newest first)
        saveHistory(context, history)
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val json = getPrefs(context).getString(HISTORY_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(HistoryItem(
                    obj.getString("key"),
                    obj.getString("oldValue"),
                    obj.getString("newValue"),
                    SettingsTable.valueOf(obj.getString("table")),
                    obj.getLong("timestamp")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(HISTORY_KEY).apply()
    }

    fun removeChange(context: Context, item: HistoryItem) {
        val history = getHistory(context).toMutableList()
        val removed = history.removeIf { it.timestamp == item.timestamp }
        if (removed) {
            saveHistory(context, history)
        }
    }

    private fun saveHistory(context: Context, list: List<HistoryItem>) {
        val array = JSONArray()
        list.take(100).forEach { item -> // Limit to last 100 changes for performance
            val obj = JSONObject().apply {
                put("key", item.key)
                put("oldValue", item.oldValue)
                put("newValue", item.newValue)
                put("table", item.table.name)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(HISTORY_KEY, array.toString()).apply()
    }
}
