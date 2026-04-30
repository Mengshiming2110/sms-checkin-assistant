package com.pengxh.smscheckin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pengxh.smscheckin.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)

        setupSectionHeaders()
        setupKeywordEditor()
        setupWhitelistEditor()
        setupNotifMonitor()
        setupDelaySlider()
        setupTimeWindow()
        setupProactiveTrigger()
        setupAlert()
        setupBatteryOptimization()
        setupWechatKeywordEditor()
        setupWechatWhitelistEditor()
        setupOverlayPermission()
        setupUpdateChecker()
    }

    override fun onResume() {
        super.onResume()
        updateBatteryOptimizationUI()
        updateOverlayUI()
        updateNotifMonitorStatus()
        refreshProactiveUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /* ========== 分区折叠 ========== */

    private fun setupSectionHeaders() {
        bindSection(binding.sectionCoreHeader, binding.sectionCoreArrow, binding.sectionCoreContent)
        bindSection(binding.sectionTimeHeader, binding.sectionTimeArrow, binding.sectionTimeContent)
        bindSection(binding.sectionPermissionHeader, binding.sectionPermissionArrow, binding.sectionPermissionContent)
        bindSection(binding.sectionOtherHeader, binding.sectionOtherArrow, binding.sectionOtherContent)
    }

    private fun bindSection(header: View, arrow: ImageView, content: View) {
        header.setOnClickListener {
            if (content.visibility == View.VISIBLE) {
                content.visibility = View.GONE
                arrow.animate().rotation(0f).setDuration(200).start()
            } else {
                content.visibility = View.VISIBLE
                arrow.animate().rotation(90f).setDuration(200).start()
            }
        }
    }

    /* ========== 关键字 ========== */

    private fun setupKeywordEditor() {
        val savedKeywordsJson = prefs.getString("keywords", null)
        val keywords = if (savedKeywordsJson != null) {
            SmsReceiver.parseKeywords(savedKeywordsJson)
        } else {
            emptyList()
        }
        SmsReceiver.keywords = keywords
        updateKeywordDisplay(keywords)

        binding.keywordCard.setOnClickListener {
            showKeywordDialog()
        }
    }

    private fun updateKeywordDisplay(keywords: List<String>) {
        if (keywords.isEmpty()) {
            binding.keywordText.text = getString(R.string.keyword_hint)
        } else if (keywords.size == 1) {
            binding.keywordText.text = keywords[0]
        } else {
            binding.keywordText.text = getString(R.string.keyword_multiple, keywords.size)
        }
    }

    private fun showKeywordDialog() {
        val savedKeywordsJson = prefs.getString("keywords", null)
        val currentKeywords = if (savedKeywordsJson != null) {
            SmsReceiver.parseKeywords(savedKeywordsJson).toMutableList()
        } else {
            mutableListOf()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        fun refreshKeywordList() {
            keywordListContainer.removeAllViews()
            if (currentKeywords.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentKeywords.forEach { keyword ->
                    val itemView = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    itemView.findViewById<TextView>(R.id.keywordText).text = keyword
                    itemView.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentKeywords.remove(keyword)
                        refreshKeywordList()
                    }
                    keywordListContainer.addView(itemView)
                }
            }
        }

        refreshKeywordList()

        inputLayout.setEndIconOnClickListener {
            val newKeyword = keywordInput.text.toString().trim()
            if (newKeyword.isNotEmpty()) {
                if (currentKeywords.contains(newKeyword)) {
                    Toast.makeText(requireContext(), R.string.keyword_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentKeywords.add(newKeyword)
                    keywordInput.text?.clear()
                    refreshKeywordList()
                    Toast.makeText(requireContext(), R.string.keyword_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val pendingText = keywordInput.text.toString().trim()
            if (pendingText.isNotEmpty() && !currentKeywords.contains(pendingText)) {
                currentKeywords.add(pendingText)
                keywordInput.text?.clear()
                refreshKeywordList()
            }
            if (currentKeywords.isEmpty()) {
                Toast.makeText(requireContext(), R.string.keyword_empty_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val json = SmsReceiver.keywordsToJson(currentKeywords)
            prefs.edit().putString("keywords", json).apply()
            SmsReceiver.keywords = currentKeywords
            SmsReceiver.notifyConfigChanged()
            updateKeywordDisplay(currentKeywords)
            Toast.makeText(requireContext(), R.string.keyword_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    /* ========== 白名单 ========== */

    private fun setupWhitelistEditor() {
        val savedWhitelistJson = prefs.getString("whitelist", null)
        val whitelist = if (savedWhitelistJson != null) {
            SmsReceiver.parseKeywords(savedWhitelistJson)
        } else {
            emptyList()
        }
        SmsReceiver.whitelist = whitelist
        updateWhitelistDisplay(whitelist)

        binding.whitelistCard.setOnClickListener {
            showWhitelistDialog()
        }
    }

    private fun updateWhitelistDisplay(whitelist: List<String>) {
        if (whitelist.isEmpty()) {
            binding.whitelistText.text = getString(R.string.whitelist_empty)
        } else {
            binding.whitelistText.text = getString(R.string.whitelist_multiple, whitelist.size)
        }
    }

    private fun showWhitelistDialog() {
        val savedWhitelistJson = prefs.getString("whitelist", null)
        val currentWhitelist = if (savedWhitelistJson != null) {
            SmsReceiver.parseKeywords(savedWhitelistJson).toMutableList()
        } else {
            mutableListOf()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        keywordInput.hint = getString(R.string.whitelist_hint)
        emptyHint.text = getString(R.string.whitelist_empty)

        fun refreshWhitelistList() {
            keywordListContainer.removeAllViews()
            if (currentWhitelist.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentWhitelist.forEach { sender ->
                    val itemView = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    itemView.findViewById<TextView>(R.id.keywordText).text = sender
                    itemView.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentWhitelist.remove(sender)
                        refreshWhitelistList()
                    }
                    keywordListContainer.addView(itemView)
                }
            }
        }

        refreshWhitelistList()

        inputLayout.setEndIconOnClickListener {
            val newSender = keywordInput.text.toString().trim()
            if (newSender.isNotEmpty()) {
                if (currentWhitelist.contains(newSender)) {
                    Toast.makeText(requireContext(), R.string.whitelist_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentWhitelist.add(newSender)
                    keywordInput.text?.clear()
                    refreshWhitelistList()
                    Toast.makeText(requireContext(), R.string.whitelist_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val json = SmsReceiver.keywordsToJson(currentWhitelist)
            prefs.edit().putString("whitelist", json).apply()
            SmsReceiver.whitelist = currentWhitelist
            SmsReceiver.notifyConfigChanged()
            updateWhitelistDisplay(currentWhitelist)
            Toast.makeText(requireContext(), R.string.whitelist_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    /* ========== 通知监听 ========== */

    private var suppressNotifSwitchListener = false

    private fun setupNotifMonitor() {
        suppressNotifSwitchListener = true
        val isEnabled = prefs.getBoolean("notif_monitor_enabled", false)
        binding.notifMonitorSwitch.isChecked = isEnabled
        suppressNotifSwitchListener = false
        updateNotifMonitorStatus()

        binding.notifMonitorSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressNotifSwitchListener) return@setOnCheckedChangeListener
            if (checked && !isNotificationListenerEnabled()) {
                suppressNotifSwitchListener = true
                binding.notifMonitorSwitch.isChecked = false
                suppressNotifSwitchListener = false
                binding.notifMonitorSettingsBtn.visibility = View.VISIBLE
                binding.notifMonitorStatusText.text = getString(R.string.notif_monitor_no_permission)
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                return@setOnCheckedChangeListener
            }
            prefs.edit().putBoolean("notif_monitor_enabled", checked).apply()
            updateNotifMonitorStatus()
        }

        binding.notifMonitorSettingsBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return flat?.contains(requireContext().packageName) == true
    }

    private fun updateNotifMonitorStatus() {
        val isEnabled = prefs.getBoolean("notif_monitor_enabled", false)
        val hasPermission = isNotificationListenerEnabled()

        suppressNotifSwitchListener = true
        binding.notifMonitorSwitch.isChecked = isEnabled && hasPermission
        suppressNotifSwitchListener = false

        if (!hasPermission) {
            binding.notifMonitorStatusText.text = getString(R.string.notif_monitor_no_permission)
            binding.notifMonitorSettingsBtn.visibility = View.VISIBLE
        } else if (isEnabled) {
            binding.notifMonitorStatusText.text = getString(R.string.notif_monitor_on)
            binding.notifMonitorSettingsBtn.visibility = View.GONE
        } else {
            binding.notifMonitorStatusText.text = getString(R.string.notif_monitor_off)
            binding.notifMonitorSettingsBtn.visibility = View.GONE
        }
    }

    /* ========== 延迟滑块 ========== */

    private fun setupDelaySlider() {
        val savedDelay = prefs.getLong("delay", 0L).coerceIn(0, 30)
        binding.delaySlider.value = savedDelay.toFloat()
        updateDelayText(savedDelay)

        binding.delaySlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val delay = value.toLong()
                prefs.edit().putLong("delay", delay).apply()
                updateDelayText(delay)
                SmsReceiver.delay = delay
                SmsReceiver.notifyConfigChanged()
            }
        }
    }

    private fun updateDelayText(delay: Long) {
        binding.delayText.text = getString(R.string.delay_format, delay)
    }

    /* ========== 打卡时间段 ========== */

    private var suppressMorningSwitchListener = false
    private var suppressEveningSwitchListener = false
    private var suppressProactiveMorningListener = false
    private var suppressProactiveEveningListener = false

    private fun setupTimeWindow() {
        val morningEnabled = prefs.getBoolean("window_morning_enabled", false)
        val morningStart = prefs.getString("window_morning_start", "09:00") ?: "09:00"
        val morningEnd = prefs.getString("window_morning_end", "10:00") ?: "10:00"

        val eveningEnabled = prefs.getBoolean("window_evening_enabled", false)
        val eveningStart = prefs.getString("window_evening_start", "18:00") ?: "18:00"
        val eveningEnd = prefs.getString("window_evening_end", "19:00") ?: "19:00"

        suppressMorningSwitchListener = true
        binding.morningWindowSwitch.isChecked = morningEnabled
        suppressMorningSwitchListener = false

        binding.morningTimeText.text = "$morningStart - $morningEnd"

        suppressEveningSwitchListener = true
        binding.eveningWindowSwitch.isChecked = eveningEnabled
        suppressEveningSwitchListener = false

        binding.eveningTimeText.text = "$eveningStart - $eveningEnd"

        updateTimeWindowDesc()

        binding.morningTimeText.setOnClickListener {
            showTimePickerDialog("morning_start", morningStart, "上班打卡开始时间") { newTime ->
                prefs.edit().putString("window_morning_start", newTime).apply()
                binding.morningTimeText.text = "$newTime - ${prefs.getString("window_morning_end", "10:00")}"
                if (prefs.getBoolean("window_morning_enabled", false)) {
                    TimeWindowReceiver.cancelWindow(requireContext(), "morning")
                    TimeWindowReceiver.scheduleWindow(requireContext(), "morning", newTime, prefs.getString("window_morning_end", "10:00") ?: "10:00")
                }
            }
        }

        binding.morningTimeText.setOnLongClickListener {
            showTimePickerDialog("morning_end", morningEnd, "上班打卡结束时间") { newTime ->
                prefs.edit().putString("window_morning_end", newTime).apply()
                binding.morningTimeText.text = "${prefs.getString("window_morning_start", "09:00")} - $newTime"
                if (prefs.getBoolean("window_morning_enabled", false)) {
                    TimeWindowReceiver.cancelWindow(requireContext(), "morning")
                    TimeWindowReceiver.scheduleWindow(requireContext(), "morning", prefs.getString("window_morning_start", "09:00") ?: "09:00", newTime)
                    ProactiveTriggerReceiver.cancelWindow(requireContext(), "morning")
                    ProactiveTriggerReceiver.scheduleWindow(requireContext(), "morning")
                }
            }
            true
        }

        binding.eveningTimeText.setOnClickListener {
            showTimePickerDialog("evening_start", eveningStart, "下班打卡开始时间") { newTime ->
                prefs.edit().putString("window_evening_start", newTime).apply()
                binding.eveningTimeText.text = "$newTime - ${prefs.getString("window_evening_end", "19:00")}"
                if (prefs.getBoolean("window_evening_enabled", false)) {
                    TimeWindowReceiver.cancelWindow(requireContext(), "evening")
                    TimeWindowReceiver.scheduleWindow(requireContext(), "evening", newTime, prefs.getString("window_evening_end", "19:00") ?: "19:00")
                }
            }
        }

        binding.eveningTimeText.setOnLongClickListener {
            showTimePickerDialog("evening_end", eveningEnd, "下班打卡结束时间") { newTime ->
                prefs.edit().putString("window_evening_end", newTime).apply()
                binding.eveningTimeText.text = "${prefs.getString("window_evening_start", "18:00")} - $newTime"
                if (prefs.getBoolean("window_evening_enabled", false)) {
                    TimeWindowReceiver.cancelWindow(requireContext(), "evening")
                    TimeWindowReceiver.scheduleWindow(requireContext(), "evening", prefs.getString("window_evening_start", "18:00") ?: "18:00", newTime)
                    ProactiveTriggerReceiver.cancelWindow(requireContext(), "evening")
                    ProactiveTriggerReceiver.scheduleWindow(requireContext(), "evening")
                }
            }
            true
        }

        binding.morningWindowSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressMorningSwitchListener) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("window_morning_enabled", checked).apply()
            if (checked) {
                TimeWindowReceiver.scheduleWindow(
                    requireContext(), "morning",
                    prefs.getString("window_morning_start", "09:00") ?: "09:00",
                    prefs.getString("window_morning_end", "10:00") ?: "10:00"
                )
                ProactiveTriggerReceiver.scheduleWindow(requireContext(), "morning")
            } else {
                TimeWindowReceiver.cancelWindow(requireContext(), "morning")
                ProactiveTriggerReceiver.cancelWindow(requireContext(), "morning")
            }
            updateTimeWindowDesc()
        }

        binding.eveningWindowSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressEveningSwitchListener) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("window_evening_enabled", checked).apply()
            if (checked) {
                TimeWindowReceiver.scheduleWindow(
                    requireContext(), "evening",
                    prefs.getString("window_evening_start", "18:00") ?: "18:00",
                    prefs.getString("window_evening_end", "19:00") ?: "19:00"
                )
                ProactiveTriggerReceiver.scheduleWindow(requireContext(), "evening")
            } else {
                TimeWindowReceiver.cancelWindow(requireContext(), "evening")
                ProactiveTriggerReceiver.cancelWindow(requireContext(), "evening")
            }
            updateTimeWindowDesc()
        }
    }

    private fun updateTimeWindowDesc() {
        val morningEnabled = prefs.getBoolean("window_morning_enabled", false)
        val eveningEnabled = prefs.getBoolean("window_evening_enabled", false)
        binding.timeWindowDescText.text = if (morningEnabled || eveningEnabled) {
            getString(R.string.timewindow_desc)
        } else {
            getString(R.string.timewindow_disabled)
        }
    }

    private fun showTimePickerDialog(key: String, currentTime: String, title: String, onTimeSet: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        android.app.TimePickerDialog(requireContext(), { _, hourOfDay, minuteOfHour ->
            val newTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
            onTimeSet(newTime)
        }, hour, minute, true).apply {
            setTitle(title)
            show()
        }
    }

    /* ========== 主动触发 ========== */

    private var suppressProactiveSwitchListener = false

    private fun setupProactiveTrigger() {
        binding.proactiveSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressProactiveSwitchListener) return@setOnCheckedChangeListener

            val serviceEnabled = prefs.getBoolean("service_enabled", false)
            if (checked && !serviceEnabled) {
                suppressProactiveSwitchListener = true
                binding.proactiveSwitch.isChecked = false
                suppressProactiveSwitchListener = false
                Toast.makeText(requireContext(), R.string.proactive_service_off, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            prefs.edit().putBoolean("proactive_enabled", checked).apply()
            refreshProactiveUI()
            if (checked) {
                ProactiveTriggerReceiver.scheduleAll(requireContext())
            } else {
                ProactiveTriggerReceiver.cancelAll(requireContext())
            }
        }

        suppressProactiveMorningListener = true
        binding.proactiveMorningSwitch.isChecked = prefs.getBoolean("proactive_morning", false)
        suppressProactiveMorningListener = false

        binding.proactiveMorningSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressProactiveMorningListener) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("proactive_morning", checked).apply()
            if (checked) {
                ProactiveTriggerReceiver.scheduleWindow(requireContext(), "morning")
            } else {
                ProactiveTriggerReceiver.cancelWindow(requireContext(), "morning")
            }
        }

        suppressProactiveEveningListener = true
        binding.proactiveEveningSwitch.isChecked = prefs.getBoolean("proactive_evening", false)
        suppressProactiveEveningListener = false

        binding.proactiveEveningSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressProactiveEveningListener) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("proactive_evening", checked).apply()
            if (checked) {
                ProactiveTriggerReceiver.scheduleWindow(requireContext(), "evening")
            } else {
                ProactiveTriggerReceiver.cancelWindow(requireContext(), "evening")
            }
        }

        val advance = prefs.getInt("proactive_advance", 60)
        binding.proactiveAdvanceSlider.value = advance.toFloat()

        binding.proactiveAdvanceSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val newAdvance = value.toInt()
                prefs.edit().putInt("proactive_advance", newAdvance).apply()
                binding.proactiveAdvanceText.text = getString(R.string.proactive_advance_format, newAdvance)
                binding.proactiveDescDetail.text = getString(R.string.proactive_desc_detail, newAdvance)
                ProactiveTriggerReceiver.cancelAll(requireContext())
                ProactiveTriggerReceiver.scheduleAll(requireContext())
            }
        }

        refreshProactiveUI()
    }

    private fun refreshProactiveUI() {
        val serviceEnabled = prefs.getBoolean("service_enabled", false)
        val proactiveEnabled = prefs.getBoolean("proactive_enabled", false)
        val advance = prefs.getInt("proactive_advance", 60)

        if (serviceEnabled) {
            binding.proactiveSwitch.isEnabled = true
            suppressProactiveSwitchListener = true
            binding.proactiveSwitch.isChecked = proactiveEnabled
            suppressProactiveSwitchListener = false
            binding.proactiveSubOptions.visibility = if (proactiveEnabled) View.VISIBLE else View.GONE
            binding.proactiveDescText.text = if (proactiveEnabled) {
                getString(R.string.proactive_desc)
            } else {
                getString(R.string.proactive_desc_off)
            }
        } else {
            binding.proactiveSwitch.isEnabled = false
            suppressProactiveSwitchListener = true
            binding.proactiveSwitch.isChecked = false
            suppressProactiveSwitchListener = false
            binding.proactiveSubOptions.visibility = View.GONE
            binding.proactiveDescText.text = getString(R.string.proactive_service_off)
        }

        binding.proactiveAdvanceText.text = getString(R.string.proactive_advance_format, advance)
        binding.proactiveDescDetail.text = getString(R.string.proactive_desc_detail, advance)
    }

    /* ========== 打卡提醒 ========== */

    private var suppressAlertSwitchListener = false

    private fun setupAlert() {
        val alertEnabled = prefs.getBoolean("alert_enabled", false)
        val alertTime = prefs.getString("alert_time", "10:00") ?: "10:00"

        suppressAlertSwitchListener = true
        binding.alertSwitch.isChecked = alertEnabled
        suppressAlertSwitchListener = false

        binding.alertTimeText.text = alertTime
        binding.alertTimeText.visibility = if (alertEnabled) View.VISIBLE else View.GONE
        binding.alertStatusText.text = if (alertEnabled) {
            getString(R.string.alert_time_format, alertTime)
        } else {
            getString(R.string.alert_disabled)
        }

        binding.alertSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressAlertSwitchListener) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("alert_enabled", checked).apply()
            if (checked) {
                val time = prefs.getString("alert_time", "10:00") ?: "10:00"
                MissedCheckInReceiver.scheduleAt(requireContext(), time)
                binding.alertTimeText.visibility = View.VISIBLE
                binding.alertStatusText.text = getString(R.string.alert_time_format, time)
            } else {
                MissedCheckInReceiver.cancel(requireContext())
                binding.alertTimeText.visibility = View.GONE
                binding.alertStatusText.text = getString(R.string.alert_disabled)
            }
        }

        binding.alertTimeText.setOnClickListener {
            val currentTime = prefs.getString("alert_time", "10:00") ?: "10:00"
            showTimePickerDialog("alert_time", currentTime, "选择提醒时间") { newTime ->
                prefs.edit().putString("alert_time", newTime).apply()
                binding.alertTimeText.text = newTime
                if (prefs.getBoolean("alert_enabled", false)) {
                    MissedCheckInReceiver.cancel(requireContext())
                    MissedCheckInReceiver.scheduleAt(requireContext(), newTime)
                }
                binding.alertStatusText.text = getString(R.string.alert_time_format, newTime)
            }
        }
    }

    /* ========== 电池优化 ========== */

    private fun setupBatteryOptimization() {
        updateBatteryOptimizationUI()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(requireContext().packageName)
        } else {
            true
        }
    }

    private fun updateBatteryOptimizationUI() {
        val isIgnoring = isIgnoringBatteryOptimizations()
        if (isIgnoring) {
            binding.batteryOptCard.visibility = View.GONE
        } else {
            binding.batteryOptCard.visibility = View.VISIBLE
            binding.batteryOptBtn.setOnClickListener {
                requestIgnoreBatteryOptimizations()
            }
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(), "无法打开设置", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /* ========== 悬浮窗权限 ========== */

    private var suppressOverlaySwitchListener = false

    private fun setupOverlayPermission() {
        suppressOverlaySwitchListener = true
        binding.overlaySwitch.isChecked = isOverlayPermissionGranted()
        suppressOverlaySwitchListener = false
        updateOverlayUI()

        binding.overlaySwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressOverlaySwitchListener) return@setOnCheckedChangeListener
            if (checked && !isOverlayPermissionGranted()) {
                suppressOverlaySwitchListener = true
                binding.overlaySwitch.isChecked = false
                suppressOverlaySwitchListener = false
                requestOverlayPermission()
            }
        }

        binding.overlayBtn.setOnClickListener {
            requestOverlayPermission()
        }
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(requireContext())
        } else {
            true
        }
    }

    private fun updateOverlayUI() {
        val granted = isOverlayPermissionGranted()
        suppressOverlaySwitchListener = true
        binding.overlaySwitch.isChecked = granted
        suppressOverlaySwitchListener = false
        if (granted) {
            binding.overlayStatusText.text = getString(R.string.overlay_granted)
            binding.overlayBtn.visibility = View.GONE
        } else {
            binding.overlayStatusText.text = getString(R.string.overlay_desc)
            binding.overlayBtn.visibility = View.VISIBLE
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法打开悬浮窗权限设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* ========== 版本更新 ========== */

    private var currentVersion: String = ""

    private fun setupUpdateChecker() {
        val pi = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        currentVersion = pi.versionName ?: "1.0"
        binding.updateStatusText.text = getString(R.string.update_current_version, currentVersion)

        binding.updateBtn.setOnClickListener {
            binding.updateBtn.isEnabled = false
            binding.updateBtn.text = getString(R.string.update_downloading)
            binding.updateStatusText.text = "正在检查..."

            UpdateChecker.checkForUpdate(requireContext()) { result ->
                requireActivity().runOnUiThread {
                    binding.updateBtn.isEnabled = true
                    binding.updateBtn.text = getString(R.string.update_check_btn)

                    result.onSuccess { info ->
                        binding.updateStatusText.text = getString(R.string.update_latest_new, info.version)
                        showUpdateDialog(info)
                    }
                    result.onFailure { e ->
                        if (e is UpdateChecker.AlreadyLatestException) {
                            binding.updateStatusText.text = getString(R.string.update_current_version, currentVersion)
                            Toast.makeText(requireContext(), R.string.update_latest_already, Toast.LENGTH_SHORT).show()
                        } else {
                            binding.updateStatusText.text = getString(R.string.update_current_version, currentVersion)
                            Toast.makeText(requireContext(), R.string.update_network_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(info: UpdateChecker.UpdateInfo) {
        val changelog = if (info.changelog.isNotEmpty()) {
            "\n\n${getString(R.string.update_changelog_title)}:\n${info.changelog}"
        } else ""

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${getString(R.string.update_latest_new, info.version)}")
            .setMessage("${getString(R.string.update_current_version, currentVersion)}$changelog")
            .setPositiveButton(R.string.update_install) { _, _ ->
                UpdateChecker.downloadAndInstall(requireContext(), info.downloadUrl)
                binding.updateStatusText.text = getString(R.string.update_downloading)
                Toast.makeText(requireContext(), getString(R.string.update_downloading), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }

    /* ========== 微信关键字 ========== */

    private fun setupWechatKeywordEditor() {
        val savedJson = prefs.getString("wechat_keywords", null)
        val keywords = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson)
        } else {
            emptyList()
        }
        SmsReceiver.wechatKeywords = keywords.toMutableList()
        updateWechatKeywordDisplay(keywords)

        binding.wechatKeywordCard.setOnClickListener {
            showWechatKeywordDialog()
        }
    }

    private fun updateWechatKeywordDisplay(keywords: List<String>) {
        if (keywords.isEmpty()) {
            binding.wechatKeywordText.text = getString(R.string.wechat_keyword_empty)
        } else if (keywords.size == 1) {
            binding.wechatKeywordText.text = keywords[0]
        } else {
            binding.wechatKeywordText.text = getString(R.string.wechat_keyword_multiple, keywords.size)
        }
    }

    private fun showWechatKeywordDialog() {
        val savedJson = prefs.getString("wechat_keywords", null)
        val currentKeywords = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson).toMutableList()
        } else {
            mutableListOf()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        keywordInput.hint = getString(R.string.keyword_hint)

        fun refreshKeywordList() {
            keywordListContainer.removeAllViews()
            if (currentKeywords.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentKeywords.forEach { keyword ->
                    val itemView = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    itemView.findViewById<TextView>(R.id.keywordText).text = keyword
                    itemView.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentKeywords.remove(keyword)
                        refreshKeywordList()
                    }
                    keywordListContainer.addView(itemView)
                }
            }
        }

        refreshKeywordList()

        inputLayout.setEndIconOnClickListener {
            val newKeyword = keywordInput.text.toString().trim()
            if (newKeyword.isNotEmpty()) {
                if (currentKeywords.contains(newKeyword)) {
                    Toast.makeText(requireContext(), R.string.wechat_keyword_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentKeywords.add(newKeyword)
                    keywordInput.text?.clear()
                    refreshKeywordList()
                    Toast.makeText(requireContext(), R.string.wechat_keyword_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val pendingText = keywordInput.text.toString().trim()
            if (pendingText.isNotEmpty() && !currentKeywords.contains(pendingText)) {
                currentKeywords.add(pendingText)
                keywordInput.text?.clear()
                refreshKeywordList()
            }
            if (currentKeywords.isEmpty()) {
                Toast.makeText(requireContext(), R.string.wechat_keyword_empty_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val json = SmsReceiver.keywordsToJson(currentKeywords)
            prefs.edit().putString("wechat_keywords", json).apply()
            SmsReceiver.wechatKeywords = currentKeywords
            SmsReceiver.notifyConfigChanged()
            updateWechatKeywordDisplay(currentKeywords)
            Toast.makeText(requireContext(), R.string.wechat_keyword_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    /* ========== 微信白名单 ========== */

    private fun setupWechatWhitelistEditor() {
        val savedJson = prefs.getString("wechat_whitelist", null)
        val whitelist = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson)
        } else {
            emptyList()
        }
        SmsReceiver.wechatWhitelist = whitelist.toMutableList()
        updateWechatWhitelistDisplay(whitelist)

        binding.wechatWhitelistCard.setOnClickListener {
            showWechatWhitelistDialog()
        }
    }

    private fun updateWechatWhitelistDisplay(whitelist: List<String>) {
        if (whitelist.isEmpty()) {
            binding.wechatWhitelistText.text = getString(R.string.wechat_whitelist_empty)
        } else {
            binding.wechatWhitelistText.text = getString(R.string.wechat_whitelist_multiple, whitelist.size)
        }
    }

    private fun showWechatWhitelistDialog() {
        val savedJson = prefs.getString("wechat_whitelist", null)
        val currentWhitelist = if (savedJson != null) {
            SmsReceiver.parseKeywords(savedJson).toMutableList()
        } else {
            mutableListOf()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        keywordInput.hint = getString(R.string.wechat_whitelist_hint)
        emptyHint.text = getString(R.string.wechat_whitelist_empty)

        fun refreshWhitelistList() {
            keywordListContainer.removeAllViews()
            if (currentWhitelist.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentWhitelist.forEach { user ->
                    val itemView = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    itemView.findViewById<TextView>(R.id.keywordText).text = user
                    itemView.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentWhitelist.remove(user)
                        refreshWhitelistList()
                    }
                    keywordListContainer.addView(itemView)
                }
            }
        }

        refreshWhitelistList()

        inputLayout.setEndIconOnClickListener {
            val newUser = keywordInput.text.toString().trim()
            if (newUser.isNotEmpty()) {
                if (currentWhitelist.contains(newUser)) {
                    Toast.makeText(requireContext(), R.string.wechat_whitelist_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentWhitelist.add(newUser)
                    keywordInput.text?.clear()
                    refreshWhitelistList()
                    Toast.makeText(requireContext(), R.string.wechat_whitelist_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val json = SmsReceiver.keywordsToJson(currentWhitelist)
            prefs.edit().putString("wechat_whitelist", json).apply()
            SmsReceiver.wechatWhitelist = currentWhitelist
            SmsReceiver.notifyConfigChanged()
            updateWechatWhitelistDisplay(currentWhitelist)
            Toast.makeText(requireContext(), R.string.wechat_whitelist_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
