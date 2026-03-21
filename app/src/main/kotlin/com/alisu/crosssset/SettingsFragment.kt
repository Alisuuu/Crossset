package com.alisu.crosssset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisu.crosssset.databinding.FragmentSettingsBinding
import kotlinx.coroutines.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private var settingsAdapter: SettingsAdapter? = null
    private lateinit var table: SettingsTable
    private var lastQuery = ""
    private var hasLoaded = false
    
    private var filterJob: Job? = null

    companion object {
        private const val ARG_TABLE = "arg_table"

        fun newInstance(table: SettingsTable): SettingsFragment {
            val fragment = SettingsFragment()
            val args = Bundle()
            args.putString(ARG_TABLE, table.name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tableName = arguments?.getString(ARG_TABLE) ?: SettingsTable.SYSTEM.name
        table = SettingsTable.valueOf(tableName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsAdapter = SettingsAdapter { item ->
            (activity as? MainActivity)?.showEditDialog(item)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = settingsAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        binding.swipeRefresh.setColorSchemeColors(resources.getColor(R.color.lilac, null))
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.surface_variant, null))
        binding.swipeRefresh.setOnRefreshListener {
            loadSettings(force = true)
        }

        // Start observing settings
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SettingsRepository.getTableFlow(table).collect { settings ->
                    if (settings.isNotEmpty()) {
                        hasLoaded = true
                        updateUI(settings)
                        binding.loadingBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    fun loadSettings(force: Boolean = false) {
        if (!isAdded) return
        
        if (!force && hasLoaded) return

        binding.loadingBar.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                SettingsRepository.loadSettings(requireContext(), table, force)
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.error_loading, table.name), Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    fun filter(query: String) {
        lastQuery = query
        if (!hasLoaded) return
        
        filterJob?.cancel()
        filterJob = viewLifecycleOwner.lifecycleScope.launch {
            val filtered = SettingsRepository.search(query, table)
            withContext(Dispatchers.Main) {
                settingsAdapter?.submitList(filtered)
            }
        }
    }

    private fun updateUI(settings: List<SettingsItem>) {
        if (lastQuery.isEmpty()) {
            settingsAdapter?.submitList(settings)
        } else {
            filter(lastQuery)
        }
    }

    override fun onDestroyView() {
        filterJob?.cancel()
        binding.recyclerView.adapter = null
        settingsAdapter = null
        super.onDestroyView()
        _binding = null
    }
}
