package com.alisu.crosssset

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alisu.crosssset.databinding.ActivityWelcomeBinding
import rikka.shizuku.Shizuku

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val shizukuRequestCode = 2000

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        updateUI()
    }

    private val onBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Se já tiver permissões e Shizuku, vai direto pra Main
        if (isShizukuGranted() && isNotificationsGranted()) {
            startMainActivity()
            return
        }

        binding.cardShizuku.setOnClickListener {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(shizukuRequestCode)
            } else {
                Toast.makeText(this, getString(R.string.shizuku_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.btnStart.setOnClickListener {
            if (isShizukuGranted()) {
                startMainActivity()
            } else {
                Toast.makeText(this, getString(R.string.shizuku_permission_not_granted), Toast.LENGTH_SHORT).show()
            }
        }

        Shizuku.addBinderReceivedListener(onBinderReceivedListener)
        updateUI()
    }

    private fun updateUI() {
        runOnUiThread {
            // Shizuku Status
            if (isShizukuGranted()) {
                binding.statusShizuku.text = getString(R.string.status_granted)
                binding.statusShizuku.setTextColor(getColor(R.color.risk_safe))
                binding.iconShizuku.setImageResource(android.R.drawable.checkbox_on_background)
                binding.iconShizuku.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.risk_safe))
            }

            // Notifications Status
            if (isNotificationsGranted()) {
                binding.statusNotifications.text = getString(R.string.status_granted)
                binding.statusNotifications.setTextColor(getColor(R.color.risk_safe))
                binding.iconNotifications.setImageResource(android.R.drawable.checkbox_on_background)
                binding.iconNotifications.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.risk_safe))
            }
            
            binding.btnStart.isEnabled = isShizukuGranted()
            binding.btnStart.alpha = if (isShizukuGranted()) 1.0f else 0.5f
        }
    }

    private fun isShizukuGranted(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(onBinderReceivedListener)
    }
}
