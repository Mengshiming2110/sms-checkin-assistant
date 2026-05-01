package com.pengxh.smscheckin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialog
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

        setupKeywordRow()
        setupWhitelistRow()
        setupWechatKeywordRow()
        setupWechatWhitelistRow()
        setupDelayRow()
        setupTimeWindowRow()
        setupProactiveTrigger()
        setupAlert()
        setupNotifMonitor()
        setupUpdateRow()
    }

    override fun onResume() {
        super.onResume()
        updateNotifMonitorStatus()
        refreshProactiveUI()
        refreshAllValueDisplays()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /* ========== 值显示刷新 ========== */

    private fun refreshAllValueDisplays() {
        val keywords = loadKeywords("keywords")
        binding.keywordValue.text = if (keywords.isEmpty()) "未设置" else "${keywords.size} 个"

        val whitelist = loadKeywords("whitelist")
        binding.whitelistValue.text = if (whitelist.isEmpty()) "不限制" else "${whitelist.size} 个"

        val wechatKeywords = loadKeywords("wechat_keywords")
        binding.wechatKeywordValue.text = if (wechatKeywords.isEmpty()) "未设置" else "${wechatKeywords.size} 个"

        val wechatWhitelist = loadKeywords("wechat_whitelist")
        binding.wechatWhitelistValue.text = if (wechatWhitelist.isEmpty()) "不监听" else "${wechatWhitelist.size} 个"

        val delay = prefs.getLong("delay", 0L).coerceIn(0, 30)
        binding.delayValue.text = "${delay} 秒"

        updateTimeWindowValue()
        updateUpdateVersion()
    }

    private fun loadKeywords(key: String): List<String> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return SmsReceiver.parseKeywords(json)
    }

    /* ========== 短信关键字 ========== */

    private fun setupKeywordRow() {
        val keywords = loadKeywords("keywords")
        SmsReceiver.keywords = keywords
        binding.keywordValue.text = if (keywords.isEmpty()) "未设置" else "${keywords.size} 个"

        binding.keywordRow.setOnClickListener { showKeywordDialog() }
    }

    private fun showKeywordDialog() {
        val currentKeywords = loadKeywords("keywords").toMutableList()
        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val keywordListContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val keywordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = keywordInput.parent.parent as com.google.android.material.textfield.TextInputLayout

        fun refresh() {
            keywordListContainer.removeAllViews()
            if (currentKeywords.isEmpty()) {
                emptyHint.visibility = View.VISIBLE
            } else {
                emptyHint.visibility = View.GONE
                currentKeywords.forEach { kw ->
                    val item = layoutInflater.inflate(R.layout.item_keyword, keywordListContainer, false)
                    item.findViewById<TextView>(R.id.keywordText).text = kw
                    item.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        currentKeywords.remove(kw)
                        refresh()
                    }
                    keywordListContainer.addView(item)
                }
            }
        }
        refresh()

        inputLayout.setEndIconOnClickListener {
            val kw = keywordInput.text.toString().trim()
            if (kw.isNotEmpty()) {
                if (currentKeywords.contains(kw)) {
                    Toast.makeText(requireContext(), R.string.keyword_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentKeywords.add(kw)
                    keywordInput.text?.clear()
                    refresh()
                    Toast.makeText(requireContext(), R.string.keyword_added, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val pending = keywordInput.text.toString().trim()
            if (pending.isNotEmpty() && !currentKeywords.contains(pending)) {
                currentKeywords.add(pending)
                keywordInput.text?.clear()
                refresh()
            }
            if (currentKeywords.isEmpty()) {
                Toast.makeText(requireContext(), R.string.keyword_empty_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val json = SmsReceiver.keywordsToJson(currentKeywords)
            prefs.edit().putString("keywords", json).apply()
            SmsReceiver.keywords = currentKeywords
            SmsReceiver.notifyConfigChanged()
            binding.keywordValue.text = "${currentKeywords.size} 个"
            Toast.makeText(requireContext(), R.string.keyword_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    /* ========== 短信白名单 ========== */

    private fun setupWhitelistRow() {
        val whitelist = loadKeywords("whitelist")
        SmsReceiver.whitelist = whitelist
        binding.whitelistValue.text = if (whitelist.isEmpty()) "不限制" else "${whitelist.size} 个"
        binding.whitelistRow.setOnClickListener { showWhitelistDialog() }
    }

    private fun showWhitelistDialog() {
        val current = loadKeywords("whitelist").toMutableList()
        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val input = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = input.parent.parent as com.google.android.material.textfield.TextInputLayout
        input.hint = getString(R.string.whitelist_hint)
        emptyHint.text = getString(R.string.whitelist_empty)

        fun refresh() {
            container.removeAllViews()
            if (current.isEmpty()) { emptyHint.visibility = View.VISIBLE }
            else {
                emptyHint.visibility = View.GONE
                current.forEach { sender ->
                    val item = layoutInflater.inflate(R.layout.item_keyword, container, false)
                    item.findViewById<TextView>(R.id.keywordText).text = sender
                    item.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        current.remove(sender); refresh()
                    }
                    container.addView(item)
                }
            }
        }
        refresh()

        inputLayout.setEndIconOnClickListener {
            val s = input.text.toString().trim()
            if (s.isNotEmpty()) {
                if (current.contains(s)) Toast.makeText(requireContext(), R.string.whitelist_exists, Toast.LENGTH_SHORT).show()
                else { current.add(s); input.text?.clear(); refresh(); Toast.makeText(requireContext(), R.string.whitelist_added, Toast.LENGTH_SHORT).show() }
            }
        }

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val pending = input.text.toString().trim()
            if (pending.isNotEmpty() && !current.contains(pending)) { current.add(pending); input.text?.clear(); refresh() }
            val json = SmsReceiver.keywordsToJson(current)
            prefs.edit().putString("whitelist", json).apply()
            SmsReceiver.whitelist = current
            SmsReceiver.notifyConfigChanged()
            binding.whitelistValue.text = if (current.isEmpty()) "不限制" else "${current.size} 个"
            Toast.makeText(requireContext(), R.string.whitelist_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    /* ========== 微信关键字 ========== */

    private fun setupWechatKeywordRow() {
        val keywords = loadKeywords("wechat_keywords")
        SmsReceiver.wechatKeywords = keywords.toMutableList()
        binding.wechatKeywordValue.text = if (keywords.isEmpty()) "未设置" else "${keywords.size} 个"
        binding.wechatKeywordRow.setOnClickListener { showWechatKeywordDialog() }
    }

    private fun showWechatKeywordDialog() {
        val current = loadKeywords("wechat_keywords").toMutableList()
        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val input = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = input.parent.parent as com.google.android.material.textfield.TextInputLayout
        input.hint = getString(R.string.keyword_hint)

        fun refresh() {
            container.removeAllViews()
            if (current.isEmpty()) { emptyHint.visibility = View.VISIBLE }
            else {
                emptyHint.visibility = View.GONE
                current.forEach { kw ->
                    val item = layoutInflater.inflate(R.layout.item_keyword, container, false)
                    item.findViewById<TextView>(R.id.keywordText).text = kw
                    item.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        current.remove(kw); refresh()
                    }
                    container.addView(item)
                }
            }
        }
        refresh()

        inputLayout.setEndIconOnClickListener {
            val kw = input.text.toString().trim()
            if (kw.isNotEmpty()) {
                if (current.contains(kw)) Toast.makeText(requireContext(), R.string.wechat_keyword_exists, Toast.LENGTH_SHORT).show()
                else { current.add(kw); input.text?.clear(); refresh(); Toast.makeText(requireContext(), R.string.wechat_keyword_added, Toast.LENGTH_SHORT).show() }
            }
        }

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val pending = input.text.toString().trim()
            if (pending.isNotEmpty() && !current.contains(pending)) { current.add(pending); input.text?.clear(); refresh() }
            if (current.isEmpty()) { Toast.makeText(requireContext(), R.string.wechat_keyword_empty_error, Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val json = SmsReceiver.keywordsToJson(current)
            prefs.edit().putString("wechat_keywords", json).apply()
            SmsReceiver.wechatKeywords = current
            SmsReceiver.notifyConfigChanged()
            binding.wechatKeywordValue.text = "${current.size} 个"
            Toast.makeText(requireContext(), R.string.wechat_keyword_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    /* ========== 微信白名单 ========== */

    private fun setupWechatWhitelistRow() {
        val whitelist = loadKeywords("wechat_whitelist")
        SmsReceiver.wechatWhitelist = whitelist.toMutableList()
        binding.wechatWhitelistValue.text = if (whitelist.isEmpty()) "不监听" else "${whitelist.size} 个"
        binding.wechatWhitelistRow.setOnClickListener { showWechatWhitelistDialog() }
    }

    private fun showWechatWhitelistDialog() {
        val current = loadKeywords("wechat_whitelist").toMutableList()
        val dialogView = layoutInflater.inflate(R.layout.dialog_keyword, null)
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.keywordListContainer)
        val input = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.keywordInput)
        val emptyHint = dialogView.findViewById<TextView>(R.id.emptyHint)
        val inputLayout = input.parent.parent as com.google.android.material.textfield.TextInputLayout
        input.hint = getString(R.string.wechat_whitelist_hint)
        emptyHint.text = getString(R.string.wechat_whitelist_empty)

        fun refresh() {
            container.removeAllViews()
            if (current.isEmpty()) { emptyHint.visibility = View.VISIBLE }
            else {
                emptyHint.visibility = View.GONE
                current.forEach { user ->
                    val item = layoutInflater.inflate(R.layout.item_keyword, container, false)
                    item.findViewById<TextView>(R.id.keywordText).text = user
                    item.findViewById<android.widget.ImageButton>(R.id.deleteButton).setOnClickListener {
                        current.remove(user); refresh()
                    }
                    container.addView(item)
                }
            }
        }
        refresh()

        inputLayout.setEndIconOnClickListener {
            val u = input.text.toString().trim()
            if (u.isNotEmpty()) {
                if (current.contains(u)) Toast.makeText(requireContext(), R.string.wechat_whitelist_exists, Toast.LENGTH_SHORT).show()
                else { current.add(u); input.text?.clear(); refresh(); Toast.makeText(requireContext(), R.string.wechat_whitelist_added, Toast.LENGTH_SHORT).show() }
            }
        }

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialogView.findViewById<android.widget.Button>(R.id.cancelButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.Button>(R.id.saveButton).setOnClickListener {
            val pending = input.text.toString().trim()
            if (pending.isNotEmpty() && !current.contains(pending)) { current.add(pending); input.text?.clear(); refresh() }
            val json = SmsReceiver.keywordsToJson(current)
            prefs.edit().putString("wechat_whitelist", json).apply()
            SmsReceiver.wechatWhitelist = current
            SmsReceiver.notifyConfigChanged()
            binding.wechatWhitelistValue.text = if (current.isEmpty()) "不监听" else "${current.size} 个"
            Toast.makeText(requireContext(), R.string.wechat_whitelist_saved, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    /* ========== 触发延迟 ========== */

    private fun setupDelayRow() {
        val delay = prefs.getLong("delay", 0L).coerceIn(0, 30)
        binding.delayValue.text = "${delay} 秒"
        binding.delayRow.setOnClickListener { showDelaySheet() }
    }

    private fun showDelaySheet() {
        val currentDelay = prefs.getLong("delay", 0L).coerceIn(0, 30)
        val root = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 48)
        }

        root.addView(TextView(requireContext()).apply {
            text = "触发延迟（${currentDelay} 秒）"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setTextColor(resources.getColor(R.color.gray_dark, null))
            setPadding(0, 0, 0, 16)
        })

        val slider = com.google.android.material.slider.Slider(requireContext()).apply {
            valueFrom = 0f; valueTo = 30f; stepSize = 1f; value = currentDelay.toFloat()
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(slider)

        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.delay_desc)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(resources.getColor(R.color.gray_text, null))
            setPadding(0, 16, 0, 0)
        })

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(root)

        slider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                val d = slider.value.toLong()
                prefs.edit().putLong("delay", d).apply()
                SmsReceiver.delay = d
                SmsReceiver.notifyConfigChanged()
                binding.delayValue.text = "${d} 秒"
                dialog.dismiss()
            }
        })
        dialog.show()
    }

    /* ========== 打卡时间段 ========== */

    private fun setupTimeWindowRow() {
        updateTimeWindowValue()
        binding.timeWindowRow.setOnClickListener { showTimeWindowDialog() }
    }

    private fun updateTimeWindowValue() {
        val morningEnabled = prefs.getBoolean("window_morning_enabled", false)
        val eveningEnabled = prefs.getBoolean("window_evening_enabled", false)
        binding.timeWindowValue.text = if (morningEnabled || eveningEnabled) {
            val ms = prefs.getString("window_morning_start", "09:00") ?: "09:00"
            val me = prefs.getString("window_morning_end", "10:00") ?: "10:00"
            val es = prefs.getString("window_evening_start", "18:00") ?: "18:00"
            val ee = prefs.getString("window_evening_end", "19:00") ?: "19:00"
            buildString {
                if (morningEnabled) append("上班 $ms-$me")
                if (morningEnabled && eveningEnabled) append(" · ")
                if (eveningEnabled) append("下班 $es-$ee")
            }
        } else {
            "未启用"
        }
    }

    private fun showTimeWindowDialog() {
        val morningEnabled = prefs.getBoolean("window_morning_enabled", false)
        val eveningEnabled = prefs.getBoolean("window_evening_enabled", false)
        val ms = prefs.getString("window_morning_start", "09:00") ?: "09:00"
        val me = prefs.getString("window_morning_end", "10:00") ?: "10:00"
        val es = prefs.getString("window_evening_start", "18:00") ?: "18:00"
        val ee = prefs.getString("window_evening_end", "19:00") ?: "19:00"

        val root = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 16)
        }

        fun addWindow(label: String, start: String, end: String, enabled: Boolean,
                      prefKey: String, onChanged: () -> Unit) {
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val labelTv = TextView(requireContext()).apply {
                text = "$label  $start - $end"
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setTextColor(resources.getColor(R.color.gray_dark, null))
            }
            val sw = com.google.android.material.materialswitch.MaterialSwitch(requireContext()).apply {
                isChecked = enabled
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(prefKey, checked).apply()
                    onChanged()
                }
            }
            row.addView(labelTv)
            row.addView(sw)
            root.addView(row)
        }

        addWindow("上班打卡", ms, me, morningEnabled, "window_morning_enabled") {
            if (prefs.getBoolean("window_morning_enabled", false)) {
                TimeWindowReceiver.scheduleWindow(requireContext(), "morning", ms, me)
            } else {
                TimeWindowReceiver.cancelWindow(requireContext(), "morning")
            }
            updateTimeWindowValue()
        }

        addWindow("下班打卡", es, ee, eveningEnabled, "window_evening_enabled") {
            if (prefs.getBoolean("window_evening_enabled", false)) {
                TimeWindowReceiver.scheduleWindow(requireContext(), "evening", es, ee)
            } else {
                TimeWindowReceiver.cancelWindow(requireContext(), "evening")
            }
            updateTimeWindowValue()
        }

        root.addView(TextView(requireContext()).apply {
            text = "点击时间文字可修改具体时间"
            setPadding(0, 12, 0, 0)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(resources.getColor(R.color.gray_text_light, null))
        })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("打卡时间段")
            .setView(root)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showTimePicker(key: String, current: String, title: String, onDone: () -> Unit) {
        val parts = current.split(":")
        android.app.TimePickerDialog(requireContext(), { _, h, m ->
            val newTime = String.format("%02d:%02d", h, m)
            prefs.edit().putString(key, newTime).apply()
            onDone()
        }, parts[0].toInt(), parts[1].toInt(), true).apply { setTitle(title); show() }
    }

    /* ========== 主动触发 ========== */

    private var suppressProactiveSwitch = false
    private var suppressProactiveMorning = false
    private var suppressProactiveEvening = false

    private fun setupProactiveTrigger() {
        binding.proactiveSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressProactiveSwitch) return@setOnCheckedChangeListener
            val serviceEnabled = prefs.getBoolean("service_enabled", false)
            if (checked && !serviceEnabled) {
                suppressProactiveSwitch = true
                binding.proactiveSwitch.isChecked = false
                suppressProactiveSwitch = false
                Toast.makeText(requireContext(), R.string.proactive_service_off, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            prefs.edit().putBoolean("proactive_enabled", checked).apply()
            refreshProactiveUI()
            if (checked) ProactiveTriggerReceiver.scheduleAll(requireContext())
            else ProactiveTriggerReceiver.cancelAll(requireContext())
        }

        suppressProactiveMorning = true
        binding.proactiveMorningSwitch.isChecked = prefs.getBoolean("proactive_morning", false)
        suppressProactiveMorning = false
        binding.proactiveMorningSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressProactiveMorning) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("proactive_morning", checked).apply()
            if (checked) ProactiveTriggerReceiver.scheduleWindow(requireContext(), "morning")
            else ProactiveTriggerReceiver.cancelWindow(requireContext(), "morning")
        }

        suppressProactiveEvening = true
        binding.proactiveEveningSwitch.isChecked = prefs.getBoolean("proactive_evening", false)
        suppressProactiveEvening = false
        binding.proactiveEveningSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressProactiveEvening) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("proactive_evening", checked).apply()
            if (checked) ProactiveTriggerReceiver.scheduleWindow(requireContext(), "evening")
            else ProactiveTriggerReceiver.cancelWindow(requireContext(), "evening")
        }

        val advance = prefs.getInt("proactive_advance", 60)
        binding.proactiveAdvanceSlider.value = advance.toFloat()
        binding.proactiveAdvanceSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val a = value.toInt()
                prefs.edit().putInt("proactive_advance", a).apply()
                binding.proactiveAdvanceText.text = "提前 ${a} 秒触发"
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

        binding.proactiveSwitch.isEnabled = serviceEnabled
        suppressProactiveSwitch = true
        binding.proactiveSwitch.isChecked = proactiveEnabled && serviceEnabled
        suppressProactiveSwitch = false
        binding.proactiveSubOptions.visibility = if (proactiveEnabled && serviceEnabled) View.VISIBLE else View.GONE
        binding.proactiveStatusText.text = if (proactiveEnabled && serviceEnabled) "已开启" else "未启用"
        if (!serviceEnabled) binding.proactiveStatusText.text = "服务未启用"
        binding.proactiveAdvanceText.text = "提前 ${advance} 秒触发"
    }

    /* ========== 打卡提醒 ========== */

    private var suppressAlertSwitch = false

    private fun setupAlert() {
        val alertEnabled = prefs.getBoolean("alert_enabled", false)
        val alertTime = prefs.getString("alert_time", "10:00") ?: "10:00"

        suppressAlertSwitch = true
        binding.alertSwitch.isChecked = alertEnabled
        suppressAlertSwitch = false

        binding.alertTimeValue.text = alertTime
        binding.alertTimeValue.visibility = if (alertEnabled) View.VISIBLE else View.GONE

        binding.alertSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressAlertSwitch) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("alert_enabled", checked).apply()
            if (checked) {
                val t = prefs.getString("alert_time", "10:00") ?: "10:00"
                MissedCheckInReceiver.scheduleAt(requireContext(), t)
                binding.alertTimeValue.visibility = View.VISIBLE
                binding.alertTimeValue.text = t
            } else {
                MissedCheckInReceiver.cancel(requireContext())
                binding.alertTimeValue.visibility = View.GONE
            }
        }

        binding.alertTimeValue.setOnClickListener {
            val current = prefs.getString("alert_time", "10:00") ?: "10:00"
            showTimePicker("alert_time", current, "选择提醒时间") {
                binding.alertTimeValue.text = prefs.getString("alert_time", "10:00")
                if (prefs.getBoolean("alert_enabled", false)) {
                    MissedCheckInReceiver.cancel(requireContext())
                    MissedCheckInReceiver.scheduleAt(requireContext(), prefs.getString("alert_time", "10:00") ?: "10:00")
                }
            }
        }
    }

    /* ========== 通知监听 ========== */

    private var suppressNotifSwitch = false

    private fun setupNotifMonitor() {
        suppressNotifSwitch = true
        binding.notifMonitorSwitch.isChecked = prefs.getBoolean("notif_monitor_enabled", false)
        suppressNotifSwitch = false
        updateNotifMonitorStatus()

        binding.notifMonitorSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressNotifSwitch) return@setOnCheckedChangeListener
            if (checked && !isNotificationListenerEnabled()) {
                suppressNotifSwitch = true
                binding.notifMonitorSwitch.isChecked = false
                suppressNotifSwitch = false
                binding.notifMonitorStatus.text = "未授权"
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                return@setOnCheckedChangeListener
            }
            prefs.edit().putBoolean("notif_monitor_enabled", checked).apply()
            updateNotifMonitorStatus()
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return flat?.contains(requireContext().packageName) == true
    }

    private fun updateNotifMonitorStatus() {
        val enabled = prefs.getBoolean("notif_monitor_enabled", false)
        val hasPerm = isNotificationListenerEnabled()

        suppressNotifSwitch = true
        binding.notifMonitorSwitch.isChecked = enabled && hasPerm
        suppressNotifSwitch = false

        binding.notifMonitorStatus.text = when {
            !hasPerm -> "未授权"
            enabled -> "已开启"
            else -> "已关闭"
        }
    }

    /* ========== 版本更新 ========== */

    private var currentVersion: String = ""

    private fun setupUpdateRow() {
        try {
            val pi = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            currentVersion = pi.versionName ?: "1.0"
        } catch (_: Exception) { currentVersion = "1.0" }
        updateUpdateVersion()
        binding.updateRow.setOnClickListener { checkUpdate() }
    }

    private fun updateUpdateVersion() {
        binding.updateVersion.text = currentVersion
    }

    private fun checkUpdate() {
        binding.updateVersion.text = "检查中..."
        UpdateChecker.checkForUpdate(requireContext()) { result ->
            requireActivity().runOnUiThread {
                binding.updateVersion.text = currentVersion
                result.onSuccess { info ->
                    binding.updateVersion.text = "发现 ${info.version}"
                    showUpdateDialog(info)
                }
                result.onFailure { e ->
                    if (e is UpdateChecker.AlreadyLatestException) {
                        Toast.makeText(requireContext(), R.string.update_latest_already, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), R.string.update_network_error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(info: UpdateChecker.UpdateInfo) {
        val changelog = if (info.changelog.isNotEmpty()) "\n\n${getString(R.string.update_changelog_title)}:\n${info.changelog}" else ""
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${getString(R.string.update_latest_new, info.version)}")
            .setMessage("${getString(R.string.update_current_version, currentVersion)}$changelog")
            .setPositiveButton(R.string.update_install) { _, _ ->
                UpdateChecker.downloadAndInstall(requireContext(), info.downloadUrl)
                binding.updateVersion.text = "下载中..."
            }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }
}

