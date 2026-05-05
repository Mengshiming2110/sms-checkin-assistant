package com.pengxh.smscheckin.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.pengxh.smscheckin.*
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val time: String,
    val category: String,
    val badgeText: String,
    val preview: String,
    val fullContent: String,
    val meta: String
)

class AppViewModel : ViewModel() {
    var serviceEnabled by mutableStateOf(false)
    var todayCount by mutableStateOf("—")
    var lastTrigger by mutableStateOf("—:—")
    var statusText by mutableStateOf("未开启")
    var dateString by mutableStateOf("")
    var uptimeText by mutableStateOf("轻触上方开关以启动监听服务")

    val logs = mutableStateListOf<LogEntry>()
    var selectedLogIndex by mutableStateOf<Int?>(null)

    // Settings state
    var keywords = mutableStateListOf<String>()
    var whitelist = mutableStateListOf<String>() // deprecated, kept for compat
    var smsEnabled by mutableStateOf(true)
    var smsNumbers = mutableStateListOf<String>()
    var wechatEnabled by mutableStateOf(true)
    var wechatContacts = mutableStateListOf<String>()
    var workStartHour by mutableStateOf("08")
    var workStartMin by mutableStateOf("00")
    var workEndHour by mutableStateOf("09")
    var workEndMin by mutableStateOf("00")
    var offWorkStartHour by mutableStateOf("18")
    var offWorkStartMin by mutableStateOf("00")
    var offWorkEndHour by mutableStateOf("19")
    var offWorkEndMin by mutableStateOf("00")
    var workToggle by mutableStateOf(true)
    var offWorkToggle by mutableStateOf(true)
    var delaySeconds by mutableIntStateOf(5)
    var proactiveToggle by mutableStateOf(true)

    private val dateFmt = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        serviceEnabled = prefs.getBoolean("service_enabled", false)

        // CRITICAL: restore global state so broadcast receivers work
        SmsReceiver.isEnabled = serviceEnabled
        if (serviceEnabled) {
            context.startForegroundService(Intent(context, CheckInForegroundService::class.java))
            DailyReportReceiver.scheduleDailyReport(context)
            KeepAliveWorker.schedule(context)
            MissedCheckInReceiver.schedule(context)
            ProactiveTriggerReceiver.scheduleAll(context)
        }

        // Load keywords
        val kwJson = prefs.getString("keywords", null)
        if (kwJson != null) {
            keywords.clear()
            keywords.addAll(SmsReceiver.parseKeywords(kwJson))
        } else {
            val defaults = listOf("打卡", "极速打卡", "上班")
            keywords.clear()
            keywords.addAll(defaults)
            prefs.edit().putString("keywords", SmsReceiver.keywordsToJson(defaults)).apply()
            SmsReceiver.keywords = defaults
        }

        // Load whitelist (deprecated)
        val wlJson = prefs.getString("whitelist", "[]")
        whitelist.clear()
        whitelist.addAll(SmsReceiver.parseKeywords(wlJson ?: "[]"))

        // Load listen source config
        smsEnabled = prefs.getBoolean("sms_enabled", true)
        val smsJson = prefs.getString("sms_numbers", "[]") ?: "[]"
        smsNumbers.clear()
        smsNumbers.addAll(SmsReceiver.parseKeywords(smsJson))
        SmsReceiver.smsEnabled = smsEnabled
        SmsReceiver.smsNumbers.clear()
        SmsReceiver.smsNumbers.addAll(smsNumbers)

        wechatEnabled = prefs.getBoolean("wechat_enabled", true)
        val wcJson = prefs.getString("wechat_contacts", "[]") ?: "[]"
        wechatContacts.clear()
        wechatContacts.addAll(SmsReceiver.parseKeywords(wcJson))
        SmsReceiver.wechatEnabled = wechatEnabled
        SmsReceiver.wechatContacts.clear()
        SmsReceiver.wechatContacts.addAll(wechatContacts)

        // Load delay
        delaySeconds = prefs.getLong("delay", 5L).toInt().coerceIn(0, 30)
        SmsReceiver.delay = delaySeconds.toLong()

        // Load time window
        val morningStart = prefs.getString("window_morning_start", "09:00") ?: "09:00"
        val morningEnd = prefs.getString("window_morning_end", "10:00") ?: "10:00"
        workStartHour = morningStart.substringBefore(":")
        workStartMin = morningStart.substringAfter(":")
        workEndHour = morningEnd.substringBefore(":")
        workEndMin = morningEnd.substringAfter(":")
        workToggle = prefs.getBoolean("window_morning_enabled", true)

        val eveningStart = prefs.getString("window_evening_start", "18:00") ?: "18:00"
        val eveningEnd = prefs.getString("window_evening_end", "19:00") ?: "19:00"
        offWorkStartHour = eveningStart.substringBefore(":")
        offWorkStartMin = eveningStart.substringAfter(":")
        offWorkEndHour = eveningEnd.substringBefore(":")
        offWorkEndMin = eveningEnd.substringAfter(":")
        offWorkToggle = prefs.getBoolean("window_evening_enabled", true)

        dateString = dateFmt.format(Date())
        refreshStats(context)
    }

    fun refreshStats(context: Context) {
        if (serviceEnabled) {
            todayCount = SmsReceiver.getTodayCount(context).toString()
            val last = SmsReceiver.getLastRecord(context)
            lastTrigger = last?.first ?: "—:—"
            statusText = "运行中"
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val startMs = prefs.getLong("service_start_time", 0L)
            if (startMs > 0) {
                val elapsed = System.currentTimeMillis() - startMs
                val hours = elapsed / 3600000
                val mins = (elapsed % 3600000) / 60000
                uptimeText = if (hours > 0) "已运行 $hours 小时 $mins 分" else "已运行 $mins 分"
            } else {
                uptimeText = "已开启打卡监听"
            }
        } else {
            todayCount = "—"
            lastTrigger = "—:—"
            statusText = "未开启"
            uptimeText = "轻触上方开关以启动监听服务"
        }
        dateString = dateFmt.format(Date())
        refreshLogs(context)
    }

    fun refreshLogs(context: Context) {
        val records = SmsReceiver.getRecords(context)
        logs.clear()
        for ((time, sender, content) in records) {
            val category = when {
                sender.contains("主动") -> "manual"
                sender.contains("通知") || sender.contains("钉钉") -> "notif"
                else -> "sms"
            }
            val badgeText = when (category) {
                "sms" -> "短信通知"
                "notif" -> "通知监听"
                else -> "主动触发"
            }
            logs.add(LogEntry(
                time = time,
                category = category,
                badgeText = badgeText,
                preview = content.take(50),
                fullContent = content,
                meta = "来源：$sender"
            ))
        }
    }
}
