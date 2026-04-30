package com.pengxh.smscheckin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "收到开机广播")
            
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val wasEnabled = prefs.getBoolean("service_enabled", false)
            
            if (wasEnabled) {
                if (!TimeWindowReceiver.isInTimeWindow(context)) {
                    Log.d(TAG, "当前不在打卡时间段内，延迟启动")
                    return
                }

                Log.d(TAG, "服务之前已启用，正在启动...")
                SmsReceiver.isEnabled = true
                
                val serviceIntent = Intent(context, CheckInForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                DailyReportReceiver.scheduleDailyReport(context)
                KeepAliveWorker.schedule(context)
                MissedCheckInReceiver.schedule(context)
                ProactiveTriggerReceiver.scheduleAll(context)
            }
        }
    }
}
