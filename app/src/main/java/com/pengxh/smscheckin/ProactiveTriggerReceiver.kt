package com.pengxh.smscheckin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProactiveTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ProactiveTrigger"
        const val ACTION_TRIGGER = "com.pengxh.smscheckin.action.PROACTIVE_TRIGGER"
        const val EXTRA_WINDOW = "window"

        fun scheduleAll(context: Context) {
            scheduleWindow(context, "morning")
            scheduleWindow(context, "evening")
        }

        fun cancelAll(context: Context) {
            cancelWindow(context, "morning")
            cancelWindow(context, "evening")
        }

        fun scheduleWindow(context: Context, window: String) {
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("proactive_enabled", false)) return
            if (!prefs.getBoolean("proactive_$window", false)) return

            val windowEndKey = "window_${window}_end"
            val defaultEnd = if (window == "morning") "10:00" else "19:00"
            val endTime = prefs.getString(windowEndKey, defaultEnd) ?: defaultEnd
            val advance = prefs.getInt("proactive_advance", 60)

            val parts = endTime.split(":")
            val endHour = parts[0].toInt()
            val endMinute = parts[1].toInt()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.SECOND, -advance)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ProactiveTriggerReceiver::class.java).apply {
                action = ACTION_TRIGGER
                putExtra(EXTRA_WINDOW, window)
            }
            val requestCode = "proactive_$window".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        fun cancelWindow(context: Context, window: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ProactiveTriggerReceiver::class.java).apply {
                action = ACTION_TRIGGER
                putExtra(EXTRA_WINDOW, window)
            }
            val requestCode = "proactive_$window".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER) return
        val window = intent.getStringExtra(EXTRA_WINDOW) ?: return

        val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("proactive_enabled", false)) {
            Log.d(TAG, "主动触发未启用")
            return
        }
        if (!prefs.getBoolean("proactive_$window", false)) {
            Log.d(TAG, "$window 窗口主动触发未启用")
            return
        }
        if (!prefs.getBoolean("service_enabled", false)) {
            Log.d(TAG, "服务未启用，跳过主动触发")
            return
        }
        if (!prefs.getBoolean("window_${window}_enabled", false)) {
            Log.d(TAG, "$window 打卡时间段未启用，跳过")
            return
        }

        val windowStart = prefs.getString(
            "window_${window}_start",
            if (window == "morning") "09:00" else "18:00"
        ) ?: "09:00"
        val windowEnd = prefs.getString(
            "window_${window}_end",
            if (window == "morning") "10:00" else "19:00"
        ) ?: "10:00"

        if (SmsReceiver.hasTriggeredInWindow(context, windowStart, windowEnd)) {
            Log.d(TAG, "$window 窗口已有触发记录，跳过主动触发")
            return
        }

        val now = System.currentTimeMillis()
        if (now - SmsReceiver.lastTriggerMs < 30_000L) {
            Log.d(TAG, "冷却中，跳过主动触发")
            return
        }
        SmsReceiver.lastTriggerMs = now

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val time = timeFormat.format(Date(now))
        val date = dateFormat.format(Date(now))

        val windowLabel = if (window == "morning") "上班" else "下班"
        SmsReceiver.saveRecord(context, time, "主动触发", "${windowLabel}窗口即将关闭，自动打开钉钉", date)
        SmsReceiver.todayTriggerCount = SmsReceiver.getTodayCount(context)
        SmsReceiver.lastTriggerTime = time
        SmsReceiver.lastSender = "主动触发"

        SmsReceiver.openDingTalk(context, "主动触发")
    }
}
