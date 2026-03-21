package com.alisu.crosssset

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class WatchedSetting(val table: SettingsTable, val key: String, val lockedValue: String)

object WatchdogManager {
    private const val PREFS_NAME = "watchdog_prefs"
    private const val WATCHDOG_KEY = "watched_settings"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getWatchedSettings(context: Context): List<WatchedSetting> {
        val json = getPrefs(context).getString(WATCHDOG_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<WatchedSetting>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(WatchedSetting(
                    SettingsTable.valueOf(obj.getString("table")),
                    obj.getString("key"),
                    obj.getString("lockedValue")
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addWatchedSetting(context: Context, setting: WatchedSetting) {
        val current = getWatchedSettings(context).filter { it.key != setting.key || it.table != setting.table }.toMutableList()
        current.add(setting)
        save(context, current)
        WatchdogService.start(context)
    }

    fun removeWatchedSetting(context: Context, table: SettingsTable, key: String) {
        val current = getWatchedSettings(context).filter { it.key != key || it.table != table }
        save(context, current)
        if (current.isEmpty()) {
            WatchdogService.stop(context)
        } else {
            WatchdogService.start(context) // Reinicia o serviço com a lista atualizada
        }
    }

    fun isWatched(context: Context, table: SettingsTable, key: String): Boolean {
        return getWatchedSettings(context).any { it.table == table && it.key == key }
    }

    private fun save(context: Context, list: List<WatchedSetting>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("table", item.table.name)
                put("key", item.key)
                put("lockedValue", item.lockedValue)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(WATCHDOG_KEY, array.toString()).apply()
    }
}
