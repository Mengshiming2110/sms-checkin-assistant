package com.pengxh.smscheckin

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.os.PowerManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifMonitor"
        private const val DING_DING_PACKAGE = "com.alibaba.android.rimet"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
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
        val prefs = getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notif_monitor_enabled", false)) return
        if (!TimeWindowReceiver.isInTimeWindow(this)) return

        when (sbn.packageName) {
            DING_DING_PACKAGE -> handleDingTalkNotification(sbn, prefs)
            WECHAT_PACKAGE -> handleWechatNotification(sbn, prefs)
        }
    }

    private fun handleDingTalkNotification(sbn: StatusBarNotification, prefs: android.content.SharedPreferences) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")

        val content = listOfNotNull(title, text)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (content.isBlank()) return

        Log.d(TAG, "收到钉钉通知: $content")

        val keywordsJson = prefs.getString("keywords", null) ?: "[]"
        val keywords = SmsReceiver.parseKeywords(keywordsJson)

        if (keywords.isEmpty()) {
            Log.d(TAG, "未配置关键字，跳过")
            return
        }

        val matchedKeyword = keywords.find { content.contains(it) }
        if (matchedKeyword == null) {
            Log.d(TAG, "通知内容未匹配关键字，跳过")
            return
        }

        performTrigger(this, prefs, "钉钉通知", content)
    }

    private fun handleWechatNotification(sbn: StatusBarNotification, prefs: android.content.SharedPreferences) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")

        if (title.isBlank() && text.isBlank()) return

        Log.d(TAG, "收到微信通知 - 发件人: $title, 内容: $text")

        val wechatWhitelistJson = prefs.getString("wechat_whitelist", "[]") ?: "[]"
        val wechatWhitelist = SmsReceiver.parseKeywords(wechatWhitelistJson)

        if (wechatWhitelist.isEmpty()) {
            Log.d(TAG, "未配置微信白名单，跳过")
            return
        }

        if (!isSenderAllowedInWechat(title, wechatWhitelist)) {
            Log.d(TAG, "微信用户 '$title' 不在白名单中，跳过")
            return
        }

        val wechatKeywordsJson = prefs.getString("wechat_keywords", "[]") ?: "[]"
        val wechatKeywords = SmsReceiver.parseKeywords(wechatKeywordsJson)

        if (wechatKeywords.isEmpty()) {
            Log.d(TAG, "未配置微信关键字，跳过")
            return
        }

        val content = listOfNotNull(title, text)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val matchedKeyword = wechatKeywords.find { content.contains(it) }
        if (matchedKeyword == null) {
            Log.d(TAG, "微信消息未匹配关键字，跳过")
            return
        }

        performTrigger(this, prefs, "微信-$title", text)
    }

    private fun performTrigger(context: Context, prefs: android.content.SharedPreferences, senderLabel: String, content: String) {
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
        SmsReceiver.saveRecord(context, time, senderLabel, content, date)

        val delay = prefs.getLong("delay", 0L)
        val delayMs = delay * 1000L

        Handler(Looper.getMainLooper()).post {
            SmsReceiver.todayTriggerCount = SmsReceiver.getTodayCount(context) + 1
            SmsReceiver.lastTriggerTime = time
            SmsReceiver.lastSender = senderLabel

            if (delayMs > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    SmsReceiver.openDingTalk(context, "通知")
                }, delayMs)
                Log.d(TAG, "延迟 ${delay}s 后打开钉钉")
            } else {
                SmsReceiver.openDingTalk(context, "通知")
            }
        }
    }

    private fun isSenderAllowedInWechat(sender: String, whitelist: List<String>): Boolean {
        return whitelist.any { sender.contains(it) || it.contains(sender) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 不需要处理
    }
}
