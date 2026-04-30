package com.pengxh.smscheckin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class TimeWindowReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ENABLE_SERVICE = "com.pengxh.smscheckin.action.ENABLE_SERVICE"
        const val ACTION_DISABLE_SERVICE = "com.pengxh.smscheckin.action.DISABLE_SERVICE"
        private const val TAG = "TimeWindowReceiver"

        fun isInTimeWindow(context: Context): Boolean {
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val calendar = Calendar.getInstance()
            val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            val morningEnabled = prefs.getBoolean("window_morning_enabled", false)
            val morningStart = prefs.getString("window_morning_start", "09:00") ?: "09:00"
            val morningEnd = prefs.getString("window_morning_end", "10:00") ?: "10:00"

            val eveningEnabled = prefs.getBoolean("window_evening_enabled", false)
            val eveningStart = prefs.getString("window_evening_start", "18:00") ?: "18:00"
            val eveningEnd = prefs.getString("window_evening_end", "19:00") ?: "19:00"

            if (!morningEnabled && !eveningEnabled) {
                return true
            }

            if (morningEnabled) {
                val startMin = parseTimeMinutes(morningStart)
                val endMin = parseTimeMinutes(morningEnd)
                if (currentMinutes in startMin..endMin) {
                    return true
                }
            }

            if (eveningEnabled) {
                val startMin = parseTimeMinutes(eveningStart)
                val endMin = parseTimeMinutes(eveningEnd)
                if (currentMinutes in startMin..endMin) {
                    return true
                }
            }

            return false
        }

        fun scheduleWindow(context: Context, key: String, startTime: String, endTime: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val startMinutes = parseTimeMinutes(startTime)
            val endMinutes = parseTimeMinutes(endTime)

            val enableIntent = Intent(context, TimeWindowReceiver::class.java).apply {
                action = ACTION_ENABLE_SERVICE
            }
            val enablePendingIntent = PendingIntent.getBroadcast(
                context, key.hashCode(), enableIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val disableIntent = Intent(context, TimeWindowReceiver::class.java).apply {
                action = ACTION_DISABLE_SERVICE
            }
            val disablePendingIntent = PendingIntent.getBroadcast(
                context, key.hashCode() + 1, disableIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                getTriggerTimeForMinutes(startMinutes),
                AlarmManager.INTERVAL_DAY,
                enablePendingIntent
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                getTriggerTimeForMinutes(endMinutes),
                AlarmManager.INTERVAL_DAY,
                disablePendingIntent
            )
        }

        fun cancelWindow(context: Context, key: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val enableIntent = Intent(context, TimeWindowReceiver::class.java).apply {
                action = ACTION_ENABLE_SERVICE
            }
            alarmManager.cancel(
                PendingIntent.getBroadcast(
                    context, key.hashCode(), enableIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val disableIntent = Intent(context, TimeWindowReceiver::class.java).apply {
                action = ACTION_DISABLE_SERVICE
            }
            alarmManager.cancel(
                PendingIntent.getBroadcast(
                    context, key.hashCode() + 1, disableIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        fun parseTimeMinutes(time: String): Int {
            val parts = time.split(":")
            return parts[0].toInt() * 60 + parts[1].toInt()
        }

        private fun getTriggerTimeForMinutes(minutes: Int): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            return calendar.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ENABLE_SERVICE -> {
                Log.d(TAG, "时间窗口开启服务")
                val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("service_enabled", true).apply()
                SmsReceiver.isEnabled = true

                val serviceIntent = Intent(context, CheckInForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                DailyReportReceiver.scheduleDailyReport(context)
                KeepAliveWorker.schedule(context)
                ProactiveTriggerReceiver.scheduleAll(context)
            }
            ACTION_DISABLE_SERVICE -> {
                Log.d(TAG, "时间窗口关闭服务")
                val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("service_enabled", false).apply()
                SmsReceiver.isEnabled = false

                context.stopService(Intent(context, CheckInForegroundService::class.java))
                DailyReportReceiver.cancelDailyReport(context)
                KeepAliveWorker.cancel(context)
                ProactiveTriggerReceiver.cancelAll(context)
            }
        }
    }
}
