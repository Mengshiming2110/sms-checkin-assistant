package com.pengxh.smscheckin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val DING_DING_PACKAGE = "com.alibaba.android.rimet"
        private const val COOLDOWN_MS = 30_000L
        private const val MAX_RECORDS = 50
        private const val MAX_CONTENT_LENGTH = 120

        var isEnabled = false
        private var keywordsList = mutableListOf<String>()
        var keywords: List<String>
            get() = keywordsList.toList()
            set(value) {
                keywordsList.clear()
                keywordsList.addAll(value)
            }

        private var whitelistList = mutableListOf<String>()
        var whitelist: List<String>
            get() = whitelistList.toList()
            set(value) {
                whitelistList.clear()
                whitelistList.addAll(value)
            }

        var delay = 0L

        private var loadedConfigVersion = -1
        var configVersion = 0
        var wechatWhitelist = mutableListOf<String>()
        var wechatKeywords = mutableListOf<String>()
        private var loadedWechatConfigVersion = -1

        fun notifyConfigChanged() {
            configVersion++
        }

        /** 共享冷却时间戳，SMS 和通知监听共用 */
        var lastTriggerMs = 0L

        var todayTriggerCount = 0
        var lastTriggerTime = ""
        var lastSender = ""

        fun parseKeywords(jsonString: String): List<String> {
            return try {
                val jsonArray = JSONArray(jsonString)
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } catch (e: Exception) {
                Log.e(TAG, "解析关键字失败", e)
                listOf("钉钉打卡")
            }
        }

        fun keywordsToJson(keywords: List<String>): String {
            val jsonArray = JSONArray()
            keywords.forEach { jsonArray.put(it) }
            return jsonArray.toString()
        }

        fun isSenderAllowed(sender: String): Boolean {
            if (whitelistList.isEmpty()) return true
            return whitelistList.any { sender.contains(it) || it.contains(sender) }
        }

        fun hasTriggeredInWindow(context: Context, windowStart: String, windowEnd: String): Boolean {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val startMin = TimeWindowReceiver.parseTimeMinutes(windowStart)
            val endMin = TimeWindowReceiver.parseTimeMinutes(windowEnd)

            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val recordsJson = prefs.getString("trigger_records", "[]") ?: "[]"
            val records = JSONArray(recordsJson)

            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                if (record.getString("date") == today) {
                    val time = record.getString("time")
                    val timeMin = TimeWindowReceiver.parseTimeMinutes(time.substring(0, 5))
                    if (timeMin in startMin..endMin) {
                        return true
                    }
                }
            }
            return false
        }

        fun saveRecord(context: Context, time: String, sender: String, content: String, date: String) {
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val recordsJson = prefs.getString("trigger_records", "[]") ?: "[]"
            val records = JSONArray(recordsJson)
            
            val record = JSONObject().apply {
                put("time", time)
                put("sender", sender)
                put("content", content.take(MAX_CONTENT_LENGTH))
                put("date", date)
                put("timestamp", System.currentTimeMillis())
            }
            
            val newRecords = JSONArray()
            newRecords.put(record)
            for (i in 0 until records.length().coerceAtMost(MAX_RECORDS - 1)) {
                newRecords.put(records.getJSONObject(i))
            }
            
            prefs.edit().putString("trigger_records", newRecords.toString()).apply()
        }

        fun getRecords(context: Context): List<Triple<String, String, String>> {
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val recordsJson = prefs.getString("trigger_records", "[]") ?: "[]"
            val records = JSONArray(recordsJson)
            val result = mutableListOf<Triple<String, String, String>>()
            
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                result.add(Triple(
                    record.getString("time"),
                    record.getString("sender"),
                    record.getString("content")
                ))
            }
            return result
        }

        fun getTodayCount(context: Context): Int {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val recordsJson = prefs.getString("trigger_records", "[]") ?: "[]"
            val records = JSONArray(recordsJson)
            var count = 0
            
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                if (record.getString("date") == today) {
                    count++
                }
            }
            return count
        }

        fun getLastRecord(context: Context): Triple<String, String, String>? {
            val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
            val recordsJson = prefs.getString("trigger_records", "[]") ?: "[]"
            val records = JSONArray(recordsJson)
            
            if (records.length() > 0) {
                val record = records.getJSONObject(0)
                return Triple(
                    record.getString("time"),
                    record.getString("sender"),
                    record.getString("content")
                )
            }
            return null
        }

        @JvmStatic
        fun openDingTalk(context: Context, source: String) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "smscheckin:trigger"
            )
            wakeLock.acquire(10_000L)

            // Android 12+ 后台启动 Activity 需要「显示悬浮窗」权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "缺少悬浮窗权限，无法在后台自动打开钉钉")
                Toast.makeText(context, "请在设置中开启「悬浮窗」权限以实现后台自动打开钉钉", Toast.LENGTH_LONG).show()
                return
            }

            try {
                val packageManager = context.packageManager
                val launchIntent = packageManager.getLaunchIntentForPackage(DING_DING_PACKAGE)

                Log.d(TAG, "尝试打开钉钉, launchIntent: $launchIntent")

                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(launchIntent)
                    Log.d(TAG, "已打开钉钉 - 方式1")
                    Toast.makeText(context, "收到打卡${source}，已打开钉钉", Toast.LENGTH_SHORT).show()
                    return
                }

                val queryIntent = Intent().apply {
                    `package` = DING_DING_PACKAGE
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfo = packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)

                Log.d(TAG, "查询钉钉应用结果: ${resolveInfo.size} 个")

                if (resolveInfo.isEmpty()) {
                    Log.w(TAG, "未找到钉钉应用")
                    Toast.makeText(context, "未找到钉钉应用，请先安装钉钉", Toast.LENGTH_LONG).show()
                    return
                }

                val activityInfo = resolveInfo[0].activityInfo
                Log.d(TAG, "钉钉启动Activity: ${activityInfo.name}")

                val intent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(DING_DING_PACKAGE, activityInfo.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                context.startActivity(intent)
                Log.d(TAG, "已打开钉钉 - 方式2")
                Toast.makeText(context, "收到打卡${source}，已打开钉钉", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "打开钉钉失败", e)
                Toast.makeText(context, "打开钉钉失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        val isServiceEnabled = prefs.getBoolean("service_enabled", false)
        if (!isServiceEnabled) {
            Log.d(TAG, "服务未启用，跳过")
            return
        }
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        if (!TimeWindowReceiver.isInTimeWindow(context)) {
            Log.d(TAG, "不在打卡时间段，跳过")
            return
        }

        if (loadedConfigVersion != configVersion) {
            val keywordsJson = prefs.getString("keywords", null) ?: "[\"钉钉打卡\"]"
            keywordsList.clear()
            keywordsList.addAll(parseKeywords(keywordsJson))

            val whitelistJson = prefs.getString("whitelist", "[]") ?: "[]"
            whitelistList.clear()
            whitelistList.addAll(parseKeywords(whitelistJson))

            delay = prefs.getLong("delay", 0L)

            val wechatWhitelistJson = prefs.getString("wechat_whitelist", "[]") ?: "[]"
            wechatWhitelist.clear()
            wechatWhitelist.addAll(parseKeywords(wechatWhitelistJson))

            val wechatKeywordsJson = prefs.getString("wechat_keywords", "[]") ?: "[]"
            wechatKeywords.clear()
            wechatKeywords.addAll(parseKeywords(wechatKeywordsJson))

            loadedWechatConfigVersion = loadedConfigVersion
            loadedConfigVersion = configVersion
        }

        val delayMs = delay * 1000L

        try {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format") ?: "3gpp"

            for (pdu in pdus) {
                val smsMessage = android.telephony.SmsMessage.createFromPdu(pdu as ByteArray, format)
                val sender = smsMessage.originatingAddress ?: ""
                val body = smsMessage.messageBody ?: ""

                Log.d(TAG, "收到短信 - 发件人: $sender, 内容: $body")

                if (!isSenderAllowed(sender)) {
                    Log.d(TAG, "发送者 '$sender' 不在白名单中，跳过")
                    continue
                }

                val matchedKeyword = keywordsList.find { body.contains(it) }
                if (matchedKeyword != null) {
                    Log.d(TAG, "检测到关键字 '$matchedKeyword'，正在处理...")
                    
                    val now = System.currentTimeMillis()
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val time = timeFormat.format(Date(now))
                    val date = dateFormat.format(Date(now))

                    todayTriggerCount = getTodayCount(context) + 1
                    lastTriggerTime = time
                    lastSender = sender

                    saveRecord(context, time, sender, body, date)

                    if (now - lastTriggerMs > COOLDOWN_MS) {
                        lastTriggerMs = now
                        if (delayMs > 0) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                openDingTalk(context, "短信")
                            }, delayMs)
                            Log.d(TAG, "延迟 ${delay}s 后打开钉钉")
                        } else {
                            openDingTalk(context, "短信")
                        }
                    } else {
                        Log.d(TAG, "冷却中，跳过打开钉钉")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理短信失败", e)
        }
    }
}
