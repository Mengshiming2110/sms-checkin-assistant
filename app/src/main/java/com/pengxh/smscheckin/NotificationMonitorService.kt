package com.pengxh.smscheckin

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifMonitor"
        private const val DING_DING_PACKAGE = "com.alibaba.android.rimet"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "通知监听服务已连接")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "通知监听服务已断开")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != DING_DING_PACKAGE) return

        val prefs = getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notif_monitor_enabled", false)) return

        if (!TimeWindowReceiver.isInTimeWindow(this)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")

        val content = listOfNotNull(title, text)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (content.isBlank()) return

        Log.d(TAG, "收到钉钉通知: $content")

        val keywordsJson = prefs.getString("keywords", null) ?: "[\"钉钉打卡\"]"
        val keywords = SmsReceiver.parseKeywords(keywordsJson)

        val matchedKeyword = keywords.find { content.contains(it) }
        if (matchedKeyword == null) {
            Log.d(TAG, "通知内容未匹配关键字，跳过")
            return
        }

        val now = System.currentTimeMillis()
        if (now - SmsReceiver.lastTriggerMs < 30_000L) {
            Log.d(TAG, "冷却中，跳过")
            return
        }
        SmsReceiver.lastTriggerMs = now

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val time = timeFormat.format(Date(now))
        val date = dateFormat.format(Date(now))
        SmsReceiver.saveRecord(this, time, "钉钉通知", content, date)

        val delay = prefs.getLong("delay", 0L)
        val delayMs = delay * 1000L

        // 切到主线程执行 UI 操作
        Handler(Looper.getMainLooper()).post {
            SmsReceiver.todayTriggerCount = SmsReceiver.getTodayCount(this) + 1
            SmsReceiver.lastTriggerTime = time
            SmsReceiver.lastSender = "钉钉通知"

            if (delayMs > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    SmsReceiver.openDingTalk(this@NotificationMonitorService, "通知")
                }, delayMs)
                Log.d(TAG, "延迟 ${delay}s 后打开钉钉")
            } else {
                SmsReceiver.openDingTalk(this@NotificationMonitorService, "通知")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 不需要处理
    }
}
