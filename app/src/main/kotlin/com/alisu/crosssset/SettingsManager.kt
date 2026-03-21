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
        
        try {
            withTimeout(5000) {
                val process = shizukuNewProcess(arrayOf("settings", "list", tableName)) ?: return@withTimeout
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0]
                            val value = parts[1]
                            result.add(SettingsItem(key, value, table, getFriendlyDescription(context, key), getRiskLevel(key)))
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
            else -> null
        }
    }

    private fun getRiskLevel(key: String): RiskLevel {
        return when {
            key.contains("animation") || key == "show_touches" -> RiskLevel.SAFE
            key == "adb_enabled" || key == "usb_mass_storage_enabled" -> RiskLevel.MODERATE
            key.contains("secure") || key.contains("password") || key.contains("lock") -> RiskLevel.DANGEROUS
            else -> RiskLevel.SAFE
        }
    }
}
