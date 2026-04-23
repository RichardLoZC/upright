package com.example.upright

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.runBlocking

class PostureMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "posture_monitor"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.example.upright.START_MONITOR"
        const val ACTION_STOP = "com.example.upright.STOP_MONITOR"
        const val ACTION_UPDATE = "com.example.upright.UPDATE_STATUS"
        const val EXTRA_STATE = "posture_state"
        const val EXTRA_LANG = "alert_language"
    }

    private var currentLang: AlertLanguage = AlertLanguage.ZH
    private val s get() = stringsFor(currentLang)

    override fun onCreate() {
        super.onCreate()
        currentLang = loadLanguage()
        createNotificationChannel()
    }

    private fun loadLanguage(): AlertLanguage {
        return try {
            val store = SettingsStore(this)
            runBlocking { store.load().alertLanguage }
        } catch (_: Exception) {
            AlertLanguage.ZH
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Update language if provided
        intent?.getStringExtra(EXTRA_LANG)?.let {
            try { currentLang = AlertLanguage.valueOf(it) } catch (_: Exception) {}
        }

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val stateName = intent.getStringExtra(EXTRA_STATE) ?: s.notifUnknown
                updateNotification(stateName)
                return START_NOT_STICKY
            }
        }

        val notification = buildNotification(s.notifRunning)
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PostureMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UpRight")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, s.notifStop, stopPending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(stateText: String) {
        val notification = buildNotification(stateText)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            s.notifChannel,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = s.notifChannelDesc
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
