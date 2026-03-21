package com.alisu.crosssset

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SettingsBackupManager {

    private const val TAG = "SettingsBackupManager"
    private const val BACKUP_FILENAME = "settings_backup.json"

    suspend fun createBackup(context: Context, outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject()
            
            SettingsTable.entries.forEach { table ->
                val settings = SettingsManager.getSettings(context, table)
                val tableArray = JSONArray()
                settings.forEach { item ->
                    val itemJson = JSONObject().apply {
                        put("key", item.key)
                        put("value", item.value)
                    }
                    tableArray.put(itemJson)
                }
                rootJson.put(table.name, tableArray)
            }

            ZipOutputStream(outputStream).use { zos ->
                val entry = ZipEntry(BACKUP_FILENAME)
                zos.putNextEntry(entry)
                zos.write(rootJson.toString(4).toByteArray())
                zos.closeEntry()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            false
        }
    }

    suspend fun restoreBackup(context: Context, inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            var jsonContent: String? = null
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == BACKUP_FILENAME) {
                        jsonContent = zis.bufferedReader().readText()
                        break
                    }
                    entry = zis.nextEntry
                }
            }

            if (jsonContent != null) {
                val rootJson = JSONObject(jsonContent!!)
                SettingsTable.entries.forEach { table ->
                    if (rootJson.has(table.name)) {
                        val tableArray = rootJson.getJSONArray(table.name)
                        for (i in 0 until tableArray.length()) {
                            val itemJson = tableArray.getJSONObject(i)
                            val key = itemJson.getString("key")
                            val value = itemJson.getString("value")
                            SettingsManager.updateSetting(table, key, value)
                        }
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            false
        }
    }
}
