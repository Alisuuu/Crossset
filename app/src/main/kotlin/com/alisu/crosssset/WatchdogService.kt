package com.alisu.crosssset

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchdogService : Service() {

    private val observers = mutableMapOf<String, ContentObserver>()

    companion object {
        private const val CHANNEL_ID = "watchdog_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WatchdogService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerObservers()
        return START_STICKY
    }

    private fun registerObservers() {
        // Unregister existing observers
        observers.values.forEach { contentResolver.unregisterContentObserver(it) }
        observers.clear()

        val watched = WatchdogManager.getWatchedSettings(this)
        if (watched.isEmpty()) {
            stopSelf()
            return
        }

        watched.forEach { setting ->
            val uri = when (setting.table) {
                SettingsTable.SYSTEM -> Settings.System.getUriFor(setting.key)
                SettingsTable.SECURE -> Settings.Secure.getUriFor(setting.key)
                SettingsTable.GLOBAL -> Settings.Global.getUriFor(setting.key)
            }

            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    enforceSetting(setting)
                }
            }

            // Detecta mudanças no URI específico
            contentResolver.registerContentObserver(uri, false, observer)
            observers[setting.key] = observer
        }
    }

    private fun enforceSetting(setting: WatchedSetting) {
        CoroutineScope(Dispatchers.IO).launch {
            // Força o valor de volta através do Shizuku
            SettingsManager.updateSetting(setting.table, setting.key, setting.lockedValue)
            Log.d("Watchdog", "Restaurado: ${setting.key} para ${setting.lockedValue}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observers.values.forEach { contentResolver.unregisterContentObserver(it) }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.watchdog_notification_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.watchdog_notification_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        
        return builder
            .setContentTitle(getString(R.string.watchdog_notification_title))
            .setContentText(getString(R.string.watchdog_notification_desc))
            .setSmallIcon(android.R.drawable.ic_secure)
            .build()
    }
}
