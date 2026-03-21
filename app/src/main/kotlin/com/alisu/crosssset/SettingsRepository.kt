package com.alisu.crosssset

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

object SettingsRepository {

    private const val TAG = "SettingsRepository"
    
    private val tableCache = mutableMapOf<SettingsTable, MutableMap<String, SettingsItem>>()
    private val tableStateFlows = mutableMapOf<SettingsTable, MutableStateFlow<List<SettingsItem>>>()

    init {
        SettingsTable.entries.forEach { table ->
            tableCache[table] = mutableMapOf()
            tableStateFlows[table] = MutableStateFlow(emptyList())
        }
    }

    fun getTableFlow(table: SettingsTable): Flow<List<SettingsItem>> = tableStateFlows[table]!!.asStateFlow()

    fun clearCache() {
        SettingsTable.entries.forEach { table ->
            tableCache[table]?.clear()
            tableStateFlows[table]?.value = emptyList()
        }
    }

    private var cachedLocale: java.util.Locale? = null

    /**
     * Carrega as configurações de uma tabela específica.
     */
    suspend fun loadSettings(context: Context, table: SettingsTable, force: Boolean = false) = withContext(Dispatchers.IO) {
        val currentLocale = context.resources.configuration.locales[0]
        val localeChanged = cachedLocale != null && cachedLocale != currentLocale
        
        if (!force && !localeChanged && tableStateFlows[table]!!.value.isNotEmpty()) return@withContext

        if (localeChanged) {
            clearCache()
        }
        cachedLocale = currentLocale

        try {
            val settings = SettingsManager.getSettings(context, table)
            val cache = tableCache[table]!!
            settings.forEach { cache[it.key] = it }
            
            val sortedList = settings.sortedBy { it.key }
            tableStateFlows[table]!!.value = sortedList
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar tabela ${table.name}", e)
        }
    }

    suspend fun search(query: String, table: SettingsTable? = null): List<SettingsItem> = withContext(Dispatchers.Default) {
        val source = if (table != null) {
            tableStateFlows[table]!!.value
        } else {
            tableStateFlows.values.flatMap { it.value }.sortedBy { it.key } // Need to sort if merging all tables
        }

        if (query.isEmpty()) return@withContext source
        
        return@withContext source.filter { 
            it.key.contains(query, ignoreCase = true) || 
            (it.description?.contains(query, ignoreCase = true) ?: false)
        }
    }

    suspend fun updateSetting(item: SettingsItem, newValue: String): Boolean = withContext(Dispatchers.IO) {
        val success = SettingsManager.updateSetting(item.table, item.key, newValue)
        if (success) {
            val updatedItem = item.copy(value = newValue)
            tableCache[item.table]?.put(item.key, updatedItem)
            
            // Update the state flow too
            val currentList = tableStateFlows[item.table]!!.value.toMutableList()
            val index = currentList.indexOfFirst { it.key == item.key }
            if (index != -1) {
                currentList[index] = updatedItem
                tableStateFlows[item.table]!!.value = currentList
            }
        }
        success
    }
}
