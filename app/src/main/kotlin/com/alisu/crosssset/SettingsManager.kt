package com.alisu.crosssset

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

object SettingsManager {

    private const val TAG = "SettingsManager"

    private val newProcessMethod: Method? by lazy {
        try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find Shizuku.newProcess via reflection", e)
            null
        }
    }

    private fun shizukuNewProcess(cmd: Array<String>): java.lang.Process? {
        return try {
            newProcessMethod?.invoke(null, cmd, null, null) as? java.lang.Process
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invoke Shizuku.newProcess", e)
            null
        }
    }

    suspend fun getSettings(context: Context, table: SettingsTable): List<SettingsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SettingsItem>()
        val tableName = table.name.lowercase()
        val noDescription = context.getString(R.string.no_description)
        
        try {
            withTimeout(5000) {
                val process = shizukuNewProcess(arrayOf("settings", "list", tableName)) ?: return@withTimeout
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0]
                            val value = parts[1]
                            val description = getFriendlyDescription(context, key) ?: noDescription
                            result.add(SettingsItem(key, value, table, description, getRiskLevel(key)))
                        }
                    }
                }
                process.waitFor()
                process.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing settings for $tableName", e)
        }
        
        result
    }

    suspend fun updateSetting(table: SettingsTable, key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        val tableName = table.name.lowercase()
        try {
            withTimeout(3000) {
                val process = shizukuNewProcess(arrayOf("settings", "put", tableName, key, value))
                val exitCode = process?.waitFor()
                process?.destroy()
                exitCode == 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating setting $key in $tableName", e)
            false
        }
    }

    private fun getFriendlyDescription(context: Context, key: String): String? {
        return when (key) {
            "animator_duration_scale" -> context.getString(R.string.desc_animator_speed)
            "window_animation_scale" -> context.getString(R.string.desc_window_speed)
            "transition_animation_scale" -> context.getString(R.string.desc_transition_speed)
            "font_scale" -> context.getString(R.string.desc_font_scale)
            "adb_enabled" -> context.getString(R.string.desc_adb_enabled)
            "show_touches" -> context.getString(R.string.desc_show_touches)
            "pointer_speed" -> context.getString(R.string.desc_pointer_speed)
            "stay_on_while_plugged_in" -> context.getString(R.string.desc_stay_on)
            "screen_brightness" -> context.getString(R.string.desc_screen_brightness)
            "screen_off_timeout" -> context.getString(R.string.desc_screen_timeout)
            "accelerometer_rotation" -> context.getString(R.string.desc_rotation)
            "data_roaming" -> context.getString(R.string.desc_roaming)
            "install_non_market_apps" -> context.getString(R.string.desc_install_non_market)
            else -> null
        }
    }

    private val dangerousKeywords = setOf("password", "lock", "pin", "encryption", "policy", "fingerprint", "provisioning", "credential", "auth")
    private val moderateKeywords = setOf("adb", "usb", "roaming", "development", "wifi_sleep", "bluetooth", "install_non_market", "tethering", "data_enabled")
    private val safeKeywords = setOf("animation", "scale", "touch", "sound", "volume", "brightness", "font", "mode", "vibrate", "timeout", "rotation")

    private fun getRiskLevel(key: String): RiskLevel {
        val k = key.lowercase()
        
        if (dangerousKeywords.any { k.contains(it) }) return RiskLevel.DANGEROUS
        if (moderateKeywords.any { k.contains(it) }) return RiskLevel.MODERATE
        if (safeKeywords.any { k.contains(it) }) return RiskLevel.SAFE
        
        return RiskLevel.SAFE
    }
}
