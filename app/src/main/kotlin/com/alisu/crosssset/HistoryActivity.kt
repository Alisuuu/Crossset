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
        binding.historyLoadingBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val success = SettingsManager.updateSetting(item.table, item.key, item.oldValue)
            binding.historyLoadingBar.visibility = View.GONE
            if (success) {
                Toast.makeText(this@HistoryActivity, R.string.revert_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@HistoryActivity, R.string.revert_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
