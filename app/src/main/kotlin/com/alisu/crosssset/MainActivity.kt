package com.alisu.crosssset

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.FrameLayout
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alisu.crosssset.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.tabs.TabLayoutMediator
import rikka.shizuku.Shizuku
import android.content.Intent
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val shizukuRequestCode = 1000
    private lateinit var pagerAdapter: SettingsPagerAdapter
    
    private var lastQuery = ""
    private var searchJob: Job? = null

    private val onBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    private val onBinderDeadListener = Shizuku.OnBinderDeadListener {
        updateShizukuStatus(false)
    }

    private val onPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        updateShizukuStatus(grantResult == PackageManager.PERMISSION_GRANTED)
    }

    private val createBackupLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { performBackup(it) }
    }

    private val pickRestoreLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { showRestoreWarning(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupViewPager()

        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                lastQuery = query
                
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    filterAllFragments(query)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        Shizuku.addBinderReceivedListener(onBinderReceivedListener)
        Shizuku.addBinderDeadListener(onBinderDeadListener)
        Shizuku.addRequestPermissionResultListener(onPermissionResultListener)

        binding.fabAdd.setOnClickListener {
            showAddSettingDialog()
        }

        checkShizukuPermission()
    }

    private fun showAddSettingDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.add_setting)

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }

        // Table Selection (Chips)
        val tableGroup = com.google.android.material.chip.ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            setPadding(0, 0, 0, 16)
        }
        
        var selectedTable = SettingsTable.SYSTEM
        SettingsTable.entries.forEach { table ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = table.name
                isCheckable = true
                if (table == SettingsTable.SYSTEM) isChecked = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedTable = table
                }
            }
            tableGroup.addView(chip)
        }
        container.addView(tableGroup)

        val keyLayout = com.google.android.material.textfield.TextInputLayout(
            this, null, com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = getString(R.string.key_hint)
            boxStrokeColor = getColor(R.color.lilac)
            setHintTextColor(android.content.res.ColorStateList.valueOf(getColor(R.color.lilac)))
            setPadding(0, 0, 0, 16)
        }
        val keyInput = TextInputEditText(keyLayout.context)
        keyLayout.addView(keyInput)
        container.addView(keyLayout)

        val valueLayout = com.google.android.material.textfield.TextInputLayout(
            this, null, com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = getString(R.string.value_hint)
            boxStrokeColor = getColor(R.color.lilac)
            setHintTextColor(android.content.res.ColorStateList.valueOf(getColor(R.color.lilac)))
        }
        val valueInput = TextInputEditText(valueLayout.context)
        valueLayout.addView(valueInput)
        container.addView(valueLayout)

        builder.setView(container)
        builder.setPositiveButton(R.string.save) { _, _ ->
            val key = keyInput.text.toString().trim()
            val value = valueInput.text.toString().trim()
            
            if (key.isNotEmpty()) {
                val item = SettingsItem(key, "", selectedTable)
                updateSetting(item, value)
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun setupViewPager() {
        pagerAdapter = SettingsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = SettingsTable.entries[position].name
        }.attach()
    }

    private fun filterAllFragments(query: String) {
        supportFragmentManager.fragments.forEach { fragment ->
            (fragment as? SettingsFragment)?.filter(query)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                createBackupLauncher.launch("crossS_backup.zip")
                true
            }
            R.id.action_restore -> {
                pickRestoreLauncher.launch(arrayOf("application/zip"))
                true
            }
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            updateShizukuStatus(false)
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            updateShizukuStatus(true)
        } else {
            updateShizukuStatus(false, getString(R.string.shizuku_permission_not_granted))
        }
    }

    private fun updateShizukuStatus(available: Boolean, message: String? = null) {
        runOnUiThread {
            if (available) {
                supportActionBar?.subtitle = getString(R.string.shizuku_active)
            } else {
                supportActionBar?.subtitle = message ?: getString(R.string.shizuku_not_available)
                if (Shizuku.pingBinder() && message != null) {
                    showPermissionDialog()
                }
            }
        }
    }

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.shizuku_dialog_title)
            .setMessage(R.string.shizuku_dialog_msg)
            .setPositiveButton(R.string.request_permission) { _, _ ->
                Shizuku.requestPermission(shizukuRequestCode)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showEditDialog(item: SettingsItem) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(item.key)
        
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }
        
        val input = TextInputEditText(this)
        input.setText(item.value)
        val params = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        input.layoutParams = params
        container.addView(input)
        
        val watchCheckBox = android.widget.CheckBox(this).apply {
            text = getString(R.string.watchdog_lock)
            isChecked = WatchdogManager.isWatched(this@MainActivity, item.table, item.key)
            setPadding(0, 16, 0, 0)
        }
        container.addView(watchCheckBox)
        
        builder.setView(container)
        builder.setMessage(item.description ?: getString(R.string.edit_setting_msg))
        
        builder.setPositiveButton(R.string.save) { _, _ ->
            val newValue = input.text.toString()
            val isWatched = watchCheckBox.isChecked
            
            if (isWatched) {
                WatchdogManager.addWatchedSetting(this, WatchedSetting(item.table, item.key, newValue))
            } else {
                WatchdogManager.removeWatchedSetting(this, item.table, item.key)
            }
            
            if (newValue != item.value) {
                updateSetting(item, newValue)
            } else if (isWatched) {
                Toast.makeText(this, getString(R.string.watchdog_enabled, item.key), Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun updateSetting(item: SettingsItem, newValue: String) {
        lifecycleScope.launch {
            val success = SettingsRepository.updateSetting(item, newValue)
            if (success) {
                HistoryManager.addChange(this@MainActivity, HistoryItem(item.key, item.value, newValue, item.table))
                Toast.makeText(this@MainActivity, R.string.setting_updated, Toast.LENGTH_SHORT).show()
                filterAllFragments(lastQuery)
            } else {
                Toast.makeText(this@MainActivity, R.string.setting_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performBackup(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { os ->
                        SettingsBackupManager.createBackup(this@MainActivity, os)
                    } ?: false
                }
                if (success) {
                    Toast.makeText(this@MainActivity, R.string.backup_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, R.string.backup_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.backup_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRestoreWarning(uri: android.net.Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.restore_warning_title)
            .setMessage(R.string.restore_warning_msg)
            .setPositiveButton(R.string.restore_action) { _, _ ->
                performRestore(uri)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performRestore(uri: android.net.Uri) {
        lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        SettingsBackupManager.restoreBackup(this@MainActivity, inputStream)
                    } ?: false
                }
                if (success) {
                    Toast.makeText(this@MainActivity, R.string.restore_complete, Toast.LENGTH_LONG).show()
                    supportFragmentManager.fragments.forEach { (it as? SettingsFragment)?.loadSettings(force = true) }
                } else {
                    Toast.makeText(this@MainActivity, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.restore_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener)
        Shizuku.removeBinderDeadListener(onBinderDeadListener)
        Shizuku.removeRequestPermissionResultListener(onPermissionResultListener)
    }
}
