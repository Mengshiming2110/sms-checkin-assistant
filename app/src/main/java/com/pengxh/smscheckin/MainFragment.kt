package com.pengxh.smscheckin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pengxh.smscheckin.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    companion object {
        private var guideShown = false
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: android.content.SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var lastLogHash = 0
    private var toggleDebounce = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableServiceInternal()
        } else {
            updateToggleUI(false)
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshStats()
            refreshLog()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)

        if (savedInstanceState == null && !guideShown) {
            guideShown = true
            showGuideDialog()
        }

        binding.serviceToggle.setOnClickListener {
            if (toggleDebounce) return@setOnClickListener
            toggleDebounce = true
            handler.postDelayed({ toggleDebounce = false }, 600)

            if (SmsReceiver.isEnabled) {
                disableService()
            } else {
                enableService()
            }
        }

        val wasServiceEnabled = prefs.getBoolean("service_enabled", false)
        if (wasServiceEnabled) {
            SmsReceiver.isEnabled = true
            requireContext().startForegroundService(Intent(requireContext(), CheckInForegroundService::class.java))
        }

        updateToggleUI(SmsReceiver.isEnabled)
        refreshStats()
        refreshLog()
    }

    override fun onResume() {
        super.onResume()
        updateToggleUI(SmsReceiver.isEnabled)
        refreshStats()
        refreshLog()
        handler.postDelayed(refreshRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateToggleUI(enabled: Boolean) {
        if (enabled) {
            binding.serviceToggle.background = resources.getDrawable(R.drawable.toggle_bg_on, null)
            binding.serviceToggleText.text = "运行中"
            binding.toggleSubText.text = "已开启打卡监听"
        } else {
            binding.serviceToggle.background = resources.getDrawable(R.drawable.toggle_bg_off, null)
            binding.serviceToggleText.text = "已暂停"
            binding.toggleSubText.text = "点击开启打卡服务"
        }
    }

    private fun enableService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        enableServiceInternal()
    }

    private fun enableServiceInternal() {
        SmsReceiver.isEnabled = true
        prefs.edit().putBoolean("service_enabled", true).apply()
        requireContext().startForegroundService(Intent(requireContext(), CheckInForegroundService::class.java))
        DailyReportReceiver.scheduleDailyReport(requireContext())
        KeepAliveWorker.schedule(requireContext())
        MissedCheckInReceiver.schedule(requireContext())
        ProactiveTriggerReceiver.scheduleAll(requireContext())
        updateToggleUI(true)
    }

    private fun disableService() {
        SmsReceiver.isEnabled = false
        prefs.edit().putBoolean("service_enabled", false).apply()
        requireContext().stopService(Intent(requireContext(), CheckInForegroundService::class.java))
        DailyReportReceiver.cancelDailyReport(requireContext())
        KeepAliveWorker.cancel(requireContext())
        MissedCheckInReceiver.cancel(requireContext())
        ProactiveTriggerReceiver.cancelAll(requireContext())
        updateToggleUI(false)
    }

    private fun showGuideDialog() {
        val message = buildString {
            appendLine(getString(R.string.guide_prerequisite_title))
            appendLine(getString(R.string.guide_step0))
            appendLine()
            appendLine(getString(R.string.guide_permission_title))
            appendLine(getString(R.string.guide_step1))
            appendLine(getString(R.string.guide_step2))
            appendLine(getString(R.string.guide_step3))
            appendLine(getString(R.string.guide_step4))
            appendLine()
            appendLine(getString(R.string.guide_usage_title))
            appendLine(getString(R.string.guide_step5))
            appendLine(getString(R.string.guide_step6))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.guide_dialog_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.guide_confirm)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun refreshStats() {
        val todayCount = SmsReceiver.getTodayCount(requireContext())
        binding.triggerCountText.text = todayCount.toString()
        SmsReceiver.todayTriggerCount = todayCount

        val lastRecord = SmsReceiver.getLastRecord(requireContext())
        if (lastRecord != null) {
            binding.lastTriggerTimeText.text = lastRecord.first
            binding.lastSenderText.text = lastRecord.second
        } else {
            binding.lastTriggerTimeText.text = "--:--"
            binding.lastSenderText.text = "--"
        }
    }

    private fun refreshLog() {
        val records = SmsReceiver.getRecords(requireContext())
        val currentHash = records.hashCode()
        if (currentHash == lastLogHash) return
        lastLogHash = currentHash

        if (records.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.logContainer.visibility = View.GONE
            return
        }

        binding.emptyState.visibility = View.GONE
        binding.logContainer.visibility = View.VISIBLE
        binding.logContainer.removeAllViews()

        for (record in records) {
            val itemView = layoutInflater.inflate(R.layout.item_log, binding.logContainer, false)
            itemView.findViewById<TextView>(R.id.timeText).text = record.first
            itemView.findViewById<TextView>(R.id.senderText).text = getString(R.string.sender_format, record.second)
            itemView.findViewById<TextView>(R.id.contentText).text = record.third
            binding.logContainer.addView(itemView)
        }
    }
}
