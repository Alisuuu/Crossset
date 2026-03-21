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
import android.content.Context
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

        binding.fabAdd.setOnClickListener { showAddSettingDialog() }
        binding.btnLegend.setOnClickListener { showLegendDialog() }

        checkShizukuPermission()
    }

    private fun showLegendDialog() {
        val message = "<b>${getString(R.string.legend_safe_title)}</b><br>" +
                      "${getString(R.string.legend_safe_desc)}<br><br>" +
                      "<b>${getString(R.string.legend_moderate_title)}</b><br>" +
                      "${getString(R.string.legend_moderate_desc)}<br><br>" +
                      "<b>${getString(R.string.legend_dangerous_title)}</b><br>" +
                      "${getString(R.string.legend_dangerous_desc)}"

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.legend_dialog_title)
            .setMessage(android.text.Html.fromHtml(message, android.text.Html.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(R.string.close, null)
            .show()
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
            R.id.action_ai_config -> {
                showAIConfigDialog()
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

    private fun showAIConfigDialog() {
        val prefs = getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.ai_config)

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }

        val providerGroup = com.google.android.material.chip.ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            setPadding(0, 0, 0, 16)
        }

        var selectedProvider = AIService.getSelectedProvider(this)
        AIService.AIProvider.entries.filter { it != AIService.AIProvider.NONE }.forEach { provider ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = provider.name
                isCheckable = true
                if (provider == selectedProvider) isChecked = true
                setOnCheckedChangeListener { _, isChecked -> if (isChecked) selectedProvider = provider }
            }
            providerGroup.addView(chip)
        }
        container.addView(providerGroup)

        val keyLayout = com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.ai_key_hint)
            boxStrokeColor = getColor(R.color.lilac)
        }
        val keyInput = TextInputEditText(keyLayout.context).apply {
            setText(AIService.getApiKey(this@MainActivity))
        }
        keyLayout.addView(keyInput)
        container.addView(keyLayout)

        builder.setView(container)
        builder.setPositiveButton(R.string.save, null) // Set to null initially to override click
        builder.setNegativeButton(R.string.cancel, null)
        val dialog = builder.create()
        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val apiKey = keyInput.text.toString().trim()
            if (apiKey.isEmpty()) {
                keyInput.error = getString(R.string.ai_no_key_error)
                return@setOnClickListener
            }

            it.isEnabled = false // Disable save button during test
            Toast.makeText(this, R.string.ai_testing_key, Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                val isValid = AIService.validateKey(selectedProvider, apiKey)
                if (isValid) {
                    prefs.edit().putString("provider", selectedProvider.name)
                        .putString("api_key", apiKey).apply()
                    Toast.makeText(this@MainActivity, R.string.ai_key_valid, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    it.isEnabled = true
                    Toast.makeText(this@MainActivity, R.string.ai_key_invalid, Toast.LENGTH_LONG).show()
                    keyInput.error = getString(R.string.ai_key_invalid)
                }
            }
        }
    }

    fun showEditDialog(item: SettingsItem) {
        val dialog = MaterialAlertDialogBuilder(this)
            .create()

        // Use a custom view for the dialog to apply glass background
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 60)
            background = getDrawable(R.drawable.bg_glass_popup)
            elevation = 20f
        }

        // Header: Flag Key + AI Button in a vertical stack to avoid overlap
        val headerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, 0, 0, 32)
        }

        val titleText = android.widget.TextView(this).apply {
            text = item.key
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setTextColor(getColor(R.color.white))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        headerLayout.addView(titleText)

        // Manual AI Button (Solid Lilac)
        val aiButton = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.ai_btn_use)
            setBackgroundColor(getColor(R.color.lilac))
            setTextColor(getColor(R.color.black))
            cornerRadius = 30
            insetTop = 0
            insetBottom = 0
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        headerLayout.addView(aiButton)
        container.addView(headerLayout)

        val noDesc = getString(R.string.no_description)
        val descText = android.widget.TextView(this).apply {
            text = item.description ?: noDesc
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 40)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        container.addView(descText)

        // AI Logic on Button Click
        aiButton.setOnClickListener {
            val provider = AIService.getSelectedProvider(this)
            val apiKey = AIService.getApiKey(this)

            if (provider == AIService.AIProvider.NONE || apiKey.isNullOrBlank()) {
                showAIHelpDialog()
            } else {
                descText.text = getString(R.string.ai_loading)
                lifecycleScope.launch {
                    val aiDesc = AIService.fetchDescription(this@MainActivity, item.table, item.key)
                    if (aiDesc != null) {
                        descText.text = aiDesc
                        DescriptionCacheManager.saveDescription(this@MainActivity, item.key, aiDesc)
                    } else {
                        descText.text = noDesc
                    }
                }
            }
        }

        // Try to load from cache first if no description
        if (item.description == noDesc || item.description.isNullOrEmpty()) {
            val cached = DescriptionCacheManager.getCachedDescription(this, item.key)
            if (cached != null) descText.text = cached
        }

        val input = TextInputEditText(this).apply {
            setText(item.value)
            setTextColor(getColor(R.color.white))
            backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.lilac))
        }
        val inputLayout = com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            boxStrokeColor = getColor(R.color.lilac)
            setHintTextColor(android.content.res.ColorStateList.valueOf(getColor(R.color.lilac)))
        }
        inputLayout.addView(input)
        container.addView(inputLayout)

        val watchCheckBox = android.widget.CheckBox(this).apply {
            text = getString(R.string.watchdog_lock)
            setTextColor(getColor(R.color.white))
            isChecked = WatchdogManager.isWatched(this@MainActivity, item.table, item.key)
            setPadding(0, 24, 0, 24)
        }
        container.addView(watchCheckBox)

        val btnRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        val btnCancel = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.cancel)
            setTextColor(getColor(R.color.text_secondary))
            setStrokeColorResource(android.R.color.transparent)
            setOnClickListener { dialog.dismiss() }
        }

        val btnSave = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.save)
            setOnClickListener {
                val newValue = input.text.toString()
                if (watchCheckBox.isChecked) {
                    WatchdogManager.addWatchedSetting(this@MainActivity, WatchedSetting(item.table, item.key, newValue))
                } else {
                    WatchdogManager.removeWatchedSetting(this@MainActivity, item.table, item.key)
                }
                if (newValue != item.value) updateSetting(item, newValue)
                dialog.dismiss()
            }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnSave)
        container.addView(btnRow)

        dialog.setView(container)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showAIHelpDialog() {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 60)
            background = getDrawable(R.drawable.bg_glass_popup)
        }

        val title = android.widget.TextView(this).apply {
            text = getString(R.string.ai_get_key_title)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setTextColor(getColor(R.color.white))
            setPadding(0, 0, 0, 24)
        }
        container.addView(title)

        val msg = android.widget.TextView(this).apply {
            text = getString(R.string.ai_get_key_msg)
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 40)
        }
        container.addView(msg)

        val providers = listOf(
            getString(R.string.ai_btn_openai) to "https://platform.openai.com/api-keys",
            getString(R.string.ai_btn_deepseek) to "https://platform.deepseek.com/api_keys",
            getString(R.string.ai_btn_gemini) to "https://aistudio.google.com/app/apikey"
        )

        providers.forEach { (name, url) ->
            val btn = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle).apply {
                text = name
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    startActivity(intent)
                }
            }
            container.addView(btn)
        }

        val btnConfig = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(R.string.ai_config)
            setTextColor(getColor(R.color.lilac))
            setStrokeColorResource(R.color.lilac)
            setOnClickListener { 
                showAIConfigDialog()
            }
        }
        container.addView(btnConfig)

        MaterialAlertDialogBuilder(this)
            .setView(container)
            .setBackground(getDrawable(android.R.color.transparent))
            .show()
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
