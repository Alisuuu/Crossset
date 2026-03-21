package com.alisu.crosssset

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object DescriptionCacheManager {
    private const val PREF_NAME = "description_cache"
    private const val CACHE_KEY = "cached_descriptions"
    private const val MAX_ITEMS = 50

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCachedDescription(context: Context, key: String): String? {
        val json = getPrefs(context).getString(CACHE_KEY, "{}")
        val cache = JSONObject(json ?: "{}")
        return if (cache.has(key)) cache.getString(key) else null
    }

    fun saveDescription(context: Context, key: String, description: String) {
        val prefs = getPrefs(context)
        val json = prefs.getString(CACHE_KEY, "{}")
        val cache = JSONObject(json ?: "{}")

        // Se chegamos no limite, limpamos o cache todo e começamos de novo (estratégia simples de limpeza)
        if (cache.length() >= MAX_ITEMS) {
            prefs.edit().clear().apply()
            val newCache = JSONObject()
            newCache.put(key, description)
            prefs.edit().putString(CACHE_KEY, newCache.toString()).apply()
        } else {
            cache.put(key, description)
            prefs.edit().putString(CACHE_KEY, cache.toString()).apply()
        }
    }
}
