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
    private var pendingPermCheck = false

    private enum class TriggerSource(val tag: String, val colorRes: Int) {
        SMS("短信", R.color.blue_primary),
        NOTIFICATION("通知", R.color.green_status),
        PROACTIVE("主动", R.color.gray_dark)
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

        if (savedInstanceState == null && !guideShown) {
            guideShown = true
            showGuideDialog()
        }

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
                            resources.getColor(R.color.blue_primary, null)
                        else
                            resources.getColor(R.color.gray_text, null)
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
                            resources.getColor(R.color.blue_primary, null)
                        else
                            resources.getColor(R.color.gray_text, null))
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
            if (enabled) R.color.gray_text else R.color.blue_primary, null
        )
        val toColor = resources.getColor(
            if (enabled) R.color.blue_primary else R.color.gray_text, null
        )

        // Smooth color transition on the oval background
        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnim.duration = 400
        colorAnim.interpolator = DecelerateInterpolator()
        colorAnim.addUpdateListener { anim ->
            (binding.serviceToggle.background as? GradientDrawable)?.setColor(anim.animatedValue as Int)
        }
        colorAnim.start()

        binding.serviceToggleText.text = if (enabled) "运行中" else "已暂停"
        binding.toggleSubText.text = if (enabled) "已开启打卡监听" else "点击开启打卡服务"
        binding.serviceToggleText.setTextColor(
            resources.getColor(if (enabled) R.color.white else R.color.gray_dark, null)
        )

        // Haptic confirmation
        binding.serviceToggle.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        // Concentric rings: scale out + fade in when enabled, reverse when disabled
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
        AlertDialog.Builder(requireContext())
            .setTitle("短信打卡助手")
            .setMessage("请先在钉钉中开启极速打卡（设置 → 考勤打卡 → 极速打卡），然后授予以下权限：\n\n1. 短信权限 — 监听打卡短信\n2. 通知权限 — 保持后台运行\n3. 悬浮窗权限 — 自动打开钉钉\n\n设置完成后打开服务开关即可。")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun refreshStats() {
        val todayCount = SmsReceiver.getTodayCount(requireContext())
        SmsReceiver.todayTriggerCount = todayCount

        val lastRecord = SmsReceiver.getLastRecord(requireContext())
        val timeStr = lastRecord?.first ?: "--:--"

        binding.statsSummary.text = "今日 ${todayCount} 次 · 最近 ${timeStr}"
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

        val count = records.size
        for ((index, record) in records.withIndex()) {
            val itemView = layoutInflater.inflate(R.layout.item_log, binding.logContainer, false)

            itemView.findViewById<TextView>(R.id.timeText).text = record.first // e.g. "08:42"

            val source = inferSource(record.second)
            val tagView = itemView.findViewById<TextView>(R.id.sourceTag)
            tagView.text = source.tag
            tagView.setTextColor(resources.getColor(source.colorRes, null))

            itemView.findViewById<TextView>(R.id.senderText).text = record.second
            itemView.findViewById<TextView>(R.id.contentText).text = record.third

            // Color the timeline dot
            val dot = itemView.findViewById<View>(R.id.timelineDot)
            val dotBg = dot.background.mutate() as? GradientDrawable
            dotBg?.setColor(resources.getColor(source.colorRes, null))

            // Hide the connecting line on the last item
            val line = itemView.findViewById<View>(R.id.timelineLine)
            if (index == count - 1) {
                line.visibility = View.INVISIBLE
            }

            binding.logContainer.addView(itemView)
        }
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
                resources.getColor(R.color.blue_primary, null)
            else
                resources.getColor(R.color.gray_text, null))
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
