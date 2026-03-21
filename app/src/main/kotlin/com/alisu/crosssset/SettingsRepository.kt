package com.alisu.crosssset

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

object SettingsRepository {

    private const val TAG = "SettingsRepository"
    
    private val tableCache = mutableMapOf<SettingsTable, MutableMap<String, SettingsItem>>()

    init {
        SettingsTable.entries.forEach { tableCache[it] = mutableMapOf() }
    }

    /**
     * Carrega as configurações de uma tabela específica.
     */
    fun loadSettings(context: Context, table: SettingsTable): Flow<List<SettingsItem>> = flow {
        try {
            val settings = SettingsManager.getSettings(context, table)
            val cache = tableCache[table]!!
            settings.forEach { cache[it.key] = it }
            
            emit(settings.sortedBy { it.key })
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar tabela ${table.name}", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun search(query: String, table: SettingsTable? = null): List<SettingsItem> = withContext(Dispatchers.Default) {
        val source = if (table != null) {
            tableCache[table]?.values ?: emptyList()
        } else {
            tableCache.values.flatMap { it.values }
        }

        if (query.isEmpty()) return@withContext source.sortedBy { it.key }
        
        return@withContext source.filter { 
            it.key.contains(query, ignoreCase = true) || 
            (it.description?.contains(query, ignoreCase = true) ?: false)
        }.sortedBy { it.key }
    }

    suspend fun updateSetting(item: SettingsItem, newValue: String): Boolean = withContext(Dispatchers.IO) {
        val success = SettingsManager.updateSetting(item.table, item.key, newValue)
        if (success) {
            val updatedItem = item.copy(value = newValue)
            tableCache[item.table]?.put(item.key, updatedItem)
        }
        success
    }
}
