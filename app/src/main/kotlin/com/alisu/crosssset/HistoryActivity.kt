package com.alisu.crosssset

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisu.crosssset.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }

        historyAdapter = HistoryAdapter { item ->
            undoChange(item)
        }

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }

        binding.btnClearHistory.setOnClickListener {
            HistoryManager.clearHistory(this)
            loadHistory()
        }

        loadHistory()
    }

    private fun loadHistory() {
        val history = HistoryManager.getHistory(this)
        historyAdapter.submitList(history)
    }

    private fun undoChange(item: HistoryItem) {
        if (!rikka.shizuku.Shizuku.pingBinder()) {
            Toast.makeText(this, R.string.shizuku_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        binding.historyLoadingBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            // Use SettingsRepository to update both the system and the local cache
            val tempItem = SettingsItem(item.key, "", item.table) 
            val success = SettingsRepository.updateSetting(tempItem, item.oldValue)
            
            if (success) {
                // Keep the loading bar visible for a moment so user sees something happened
                kotlinx.coroutines.delay(500)
                binding.historyLoadingBar.visibility = View.GONE
                
                HistoryManager.removeChange(this@HistoryActivity, item)
                loadHistory() // Refresh the list
                Toast.makeText(this@HistoryActivity, R.string.revert_success, Toast.LENGTH_SHORT).show()
                android.util.Log.d("HistoryActivity", "Successfully reverted ${item.key} to ${item.oldValue}")
            } else {
                binding.historyLoadingBar.visibility = View.GONE
                Toast.makeText(this@HistoryActivity, R.string.revert_error, Toast.LENGTH_SHORT).show()
                android.util.Log.e("HistoryActivity", "Failed to revert ${item.key}")
            }
        }
    }
}
