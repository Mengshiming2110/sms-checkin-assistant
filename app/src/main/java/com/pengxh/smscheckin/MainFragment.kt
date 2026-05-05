// LEGACY: This Fragment-based UI is superseded by the Compose UI in MainActivity + ui/screens/.
// Kept for reference. Not instantiated by the current activity.
package com.pengxh.smscheckin

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.pengxh.smscheckin.databinding.FragmentMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var pendingPermCheck = false
    private val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE)

    private enum class TriggerSource(val tag: String, val badgeBgRes: Int, val textColorRes: Int) {
        SMS("短信通知", R.drawable.badge_sms, R.color.terracotta),
        NOTIFICATION("通知监听", R.drawable.badge_notif, R.color.green_status),
        PROACTIVE("主动触发", R.drawable.badge_manual, R.color.gray_text)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) checkSmsPermissionThenEnable()
        else updateToggleUI(false)
    }

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) checkNotificationThenEnable()
        else updateToggleUI(false)
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

        binding.dateText.text = dateFormat.format(Date())

        if (savedInstanceState == null && !guideShown) {
            guideShown = true
            showGuideDialog()
        }

        binding.guideButton.setOnClickListener { showGuideDialog() }

        binding.serviceToggle.setOnClickListener {
            if (toggleDebounce) return@setOnClickListener
            toggleDebounce = true
            handler.postDelayed({ toggleDebounce = false }, 600)

            // Press-release spring animation
            animateTogglePress(binding.serviceToggle) {
                if (SmsReceiver.isEnabled) disableService() else enableService()
            }
        }

        // Touch listener: physical button feel via translationZ + subtle scale
        binding.serviceToggle.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    val sink = -dpToPx(5f)
                    v.animate()
                        .scaleX(0.97f).scaleY(0.97f)
                        .translationZ(sink)
                        .setDuration(100)
                        .start()
                    (v.background as? GradientDrawable)?.let { bg ->
                        val current = if (SmsReceiver.isEnabled)
                            resources.getColor(R.color.terracotta, null)
                        else
                            resources.getColor(R.color.gray_text_light, null)
                        bg.setColor(darkenColor(current, 0.12f))
                    }
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .translationZ(0f)
                        .setDuration(200)
                        .start()
                    (v.background as? GradientDrawable)?.let { bg ->
                        bg.setColor(if (SmsReceiver.isEnabled)
                            resources.getColor(R.color.terracotta, null)
                        else
                            resources.getColor(R.color.gray_text_light, null))
                    }
                }
            }
            false
        }

        // Oval outline so the 160dp circle casts a circular shadow
        binding.serviceToggle.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }

        val wasServiceEnabled = prefs.getBoolean("service_enabled", false)
        if (wasServiceEnabled) {
            SmsReceiver.isEnabled = true
            requireContext().startForegroundService(Intent(requireContext(), CheckInForegroundService::class.java))
            ensureSystemPermissions()
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

        // Continue system permission flow after returning from settings
        if (pendingPermCheck) {
            pendingPermCheck = false
            if (isOverlayGranted() && !isIgnoringBattery()) {
                showBatteryDialog()
            }
        }
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
        val bg = (binding.serviceToggle.background as? GradientDrawable)
        val fromColor = bg?.color?.defaultColor ?: resources.getColor(
            if (enabled) R.color.gray_text_light else R.color.terracotta, null
        )
        val toColor = resources.getColor(
            if (enabled) R.color.terracotta else R.color.gray_text_light, null
        )

        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnim.duration = 400
        colorAnim.interpolator = DecelerateInterpolator()
        colorAnim.addUpdateListener { anim ->
            (binding.serviceToggle.background as? GradientDrawable)?.setColor(anim.animatedValue as Int)
        }
        colorAnim.start()

        binding.serviceToggleText.text = if (enabled) "运行中" else "已暂停"
        binding.toggleSubText.text = if (enabled) "已开启打卡监听" else "轻触上方开关以启动监听服务"
        binding.serviceToggleText.setTextColor(
            resources.getColor(if (enabled) R.color.white else R.color.gray_dark, null)
        )

        // Status dot
        binding.statusDot.backgroundTintList = resources.getColorStateList(
            if (enabled) R.color.green_status else R.color.gray_text_light, null
        )

        // Toggle title
        binding.toggleTitle.text = if (enabled) "服务运行中" else "服务未开启"
        binding.toggleTitle.setTextColor(
            resources.getColor(if (enabled) R.color.gray_dark else R.color.gray_text_light, null)
        )

        // Stats strip dimming
        binding.statsStrip.alpha = if (enabled) 1f else 0.4f

        // Log section dimming
        binding.logSection.alpha = if (enabled) 1f else 0.45f

        binding.serviceToggle.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        if (enabled) {
            binding.concentricRings.visibility = View.VISIBLE
            binding.concentricRings.alpha = 0f
            binding.concentricRings.scaleX = 0.6f
            binding.concentricRings.scaleY = 0.6f
            binding.concentricRings.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            binding.concentricRings.animate()
                .alpha(0f)
                .scaleX(0.6f).scaleY(0.6f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    binding.concentricRings.visibility = View.GONE
                }
                .start()
        }
    }

    private fun enableService() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            return
        }
        checkNotificationThenEnable()
    }

    private fun checkSmsPermissionThenEnable() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            checkNotificationThenEnable()
        } else {
            updateToggleUI(false)
        }
    }

    private fun checkNotificationThenEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
        ensureSystemPermissions()
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
        val message = "① 前置条件\n" +
                "请确认钉钉已开启极速打卡功能，并保持后台运行。\n\n" +
                "② 必须权限\n" +
                "短信读取权限 · 通知监听权限\n" +
                "两项均为系统级授权，仅用于匹配关键字触发打卡。\n\n" +
                "③ 使用方法\n" +
                "安装授权 → 配置触发关键字 → 开启服务开关 → 设好即忘。"
        AlertDialog.Builder(requireContext())
            .setTitle("欢迎使用短信打卡助手")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun refreshStats() {
        val todayCount = SmsReceiver.getTodayCount(requireContext())
        SmsReceiver.todayTriggerCount = todayCount

        val lastRecord = SmsReceiver.getLastRecord(requireContext())
        val timeStr = lastRecord?.first ?: "--:--"

        val enabled = SmsReceiver.isEnabled

        binding.statCount.text = if (enabled) todayCount.toString() else "—"
        binding.statLast.text = if (enabled) timeStr else "—:—"
        binding.statStatus.text = if (enabled) "运行中" else "未开启"
        binding.statStatus.textSize = if (enabled) 22f else 16f
    }

    private fun refreshLog() {
        val records = SmsReceiver.getRecords(requireContext())
        val currentHash = records.hashCode()
        if (currentHash == lastLogHash) return
        lastLogHash = currentHash

        val enabled = SmsReceiver.isEnabled

        if (records.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.logSection.visibility = View.GONE
            return
        }

        binding.emptyState.visibility = View.GONE
        binding.logSection.visibility = View.VISIBLE
        binding.logSection.alpha = if (enabled) 1f else 0.45f
        binding.logContainer.removeAllViews()

        for ((index, record) in records.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_log, binding.logContainer, false)

            itemView.findViewById<TextView>(R.id.timeText).text = record.first

            val source = inferSource(record.second)
            val tagView = itemView.findViewById<TextView>(R.id.sourceTag)
            tagView.text = source.tag
            tagView.setBackgroundResource(source.badgeBgRes)
            tagView.setTextColor(resources.getColor(source.textColorRes, null))

            itemView.findViewById<TextView>(R.id.senderText).text = record.third

            // Log detail on click
            itemView.setOnClickListener {
                showLogDetail(record)
            }

            binding.logContainer.addView(itemView)
        }

        if (binding.logContainer.childCount > 0) {
            // Remove bottom margin from last item
            val lastChild = binding.logContainer.getChildAt(binding.logContainer.childCount - 1)
            val lp = lastChild.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            lp?.bottomMargin = 0
        }
    }

    private fun showLogDetail(record: Triple<String, String, String>) {
        val source = inferSource(record.second)
        val message = "时间：${record.first}\n\n来源：${record.second}\n\n内容：${record.third}\n\n触发方式：${source.tag}"
        AlertDialog.Builder(requireContext())
            .setTitle("触发详情")
            .setMessage(message)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun inferSource(sender: String): TriggerSource {
        return when {
            sender.contains("主动") -> TriggerSource.PROACTIVE
            sender.contains("通知") || sender.contains("钉钉") -> TriggerSource.NOTIFICATION
            else -> TriggerSource.SMS
        }
    }

    private fun animateTogglePress(view: View, onComplete: () -> Unit) {
        (view.background as? GradientDrawable)?.let { bg ->
            bg.setColor(if (SmsReceiver.isEnabled)
                resources.getColor(R.color.terracotta, null)
            else
                resources.getColor(R.color.gray_text_light, null))
        }
        view.animate()
            .scaleX(1f).scaleY(1f)
            .translationZ(0f)
            .setDuration(350)
            .setInterpolator(OvershootInterpolator(0.3f))
            .withEndAction {
                view.parent.requestDisallowInterceptTouchEvent(false)
                handler.postDelayed({ onComplete() }, 60)
            }
            .start()
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16) and 0xFF) * (1f - factor)
        val g = ((color shr 8) and 0xFF) * (1f - factor)
        val b = (color and 0xFF) * (1f - factor)
        return (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    /* ========== 系统权限启动时请求 ========== */

    private fun ensureSystemPermissions() {
        if (!isOverlayGranted()) {
            pendingPermCheck = true
            AlertDialog.Builder(requireContext())
                .setTitle("悬浮窗权限")
                .setMessage("用于在触发打卡时自动打开钉钉，无需手动操作")
                .setPositiveButton("去授权") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${requireContext().packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                .setNegativeButton("跳过") { _, _ ->
                    pendingPermCheck = false
                    if (!isIgnoringBattery()) showBatteryDialog()
                }
                .show()
        } else if (!isIgnoringBattery()) {
            showBatteryDialog()
        }
    }

    private fun showBatteryDialog() {
        pendingPermCheck = true
        AlertDialog.Builder(requireContext())
            .setTitle("电池优化")
            .setMessage("关闭电池优化可防止系统在后台终止打卡服务")
            .setPositiveButton("去设置") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
            .setNegativeButton("跳过") { _, _ -> pendingPermCheck = false }
            .show()
    }

    private fun isOverlayGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(requireContext())
        else true
    }

    private fun isIgnoringBattery(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(requireContext().packageName)
        } else true
    }
}
