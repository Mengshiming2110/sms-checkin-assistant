package com.pengxh.smscheckin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class CheckInForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "sms_checkin_foreground"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISABLE = "com.pengxh.smscheckin.DISABLE_FROM_NOTIF"
        private const val UPDATE_INTERVAL_MS = 10_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISABLE) {
            stopSelf()
            return START_NOT_STICKY
        }
        SmsReceiver.isEnabled = true
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS)
        return START_STICKY
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("sms_checkin_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("service_enabled", false).apply()
        SmsReceiver.isEnabled = false
        DailyReportReceiver.cancelDailyReport(this)
        KeepAliveWorker.cancel(this)
        ProactiveTriggerReceiver.cancelAll(this)
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val count = SmsReceiver.todayTriggerCount
        val lastTime = SmsReceiver.lastTriggerTime.ifEmpty { "--:--:--" }

        val content = if (count > 0) {
            getString(R.string.notif_content_with_count, count, lastTime)
        } else {
            getString(R.string.notif_content_idle)
        }

        val disableIntent = Intent(this, CheckInForegroundService::class.java).apply {
            action = ACTION_DISABLE
        }
        val disablePendingIntent = PendingIntent.getService(
            this, 0, disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_pause, getString(R.string.notif_action_disable), disablePendingIntent)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }
}
