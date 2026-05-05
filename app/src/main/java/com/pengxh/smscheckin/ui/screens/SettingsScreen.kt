package com.pengxh.smscheckin.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pengxh.smscheckin.ProactiveTriggerReceiver
import com.pengxh.smscheckin.SmsReceiver
import com.pengxh.smscheckin.ui.components.*
import com.pengxh.smscheckin.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: AppViewModel = viewModel(),
    onNavigateToKeywords: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onOpenTimePicker: (String) -> Unit,
    onOpenPermissionDetail: (String) -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE) }

    val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val notifGranted = remember {
        val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        listeners.contains(context.packageName)
    }
    val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    val batteryOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
    } else true

    val allGranted = smsGranted && notifGranted && overlayGranted && batteryOptimized

    val kwSummary = remember(viewModel.keywords.toList()) {
        viewModel.keywords.take(3).joinToString("、") +
                if (viewModel.keywords.size > 3) "…" else ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
            Text("偏好设置", fontSize = 12.sp, color = Muted)
            Text("设置", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).sp, color = Fg)
        }

        Spacer(Modifier.height(8.dp))

        IosSection("核心配置") {
            IosRow(
                icon = IconSearch,
                title = "管理触发关键字",
                value = kwSummary,
                showChevron = true,
                isFirst = true,
                onClick = onNavigateToKeywords
            )
            val sourceSummary = remember(viewModel.smsEnabled, viewModel.wechatEnabled) {
                val sources = mutableListOf<String>()
                if (viewModel.smsEnabled) sources.add("短信")
                if (viewModel.wechatEnabled) sources.add("微信")
                if (sources.isEmpty()) "未开启" else sources.joinToString(" · ")
            }
            IosRow(
                icon = IconBlock,
                title = "监听来源",
                value = sourceSummary,
                showChevron = true,
                isLast = true,
                onClick = onNavigateToWhitelist
            )
        }

        IosSection("时间策略") {
            IosRow(
                icon = IconClock,
                title = "上班打卡",
                subtitle = "工作日 ${viewModel.workStartHour}:${viewModel.workStartMin} — ${viewModel.workEndHour}:${viewModel.workEndMin} 自动触发",
                showToggle = true,
                toggleOn = viewModel.workToggle,
                onToggle = {
                    viewModel.workToggle = it
                    prefs.edit().putBoolean("window_morning_enabled", it).apply()
                },
                isFirst = true,
                onClick = { onOpenTimePicker("work-start") }
            )
            IosRow(
                icon = IconClock,
                title = "下班打卡",
                subtitle = "工作日 ${viewModel.offWorkStartHour}:${viewModel.offWorkStartMin} — ${viewModel.offWorkEndHour}:${viewModel.offWorkEndMin} 自动触发",
                showToggle = true,
                toggleOn = viewModel.offWorkToggle,
                onToggle = {
                    viewModel.offWorkToggle = it
                    prefs.edit().putBoolean("window_evening_enabled", it).apply()
                },
                onClick = { onOpenTimePicker("work-end") }
            )

            IosSlider(
                value = viewModel.delaySeconds,
                onValueChange = { viewModel.delaySeconds = it },
                onValueChangeFinished = {
                    prefs.edit().putLong("delay", viewModel.delaySeconds.toLong()).apply()
                    SmsReceiver.delay = viewModel.delaySeconds.toLong()
                    SmsReceiver.notifyConfigChanged()
                }
            )

            IosRow(
                icon = IconTimer,
                title = "主动触发",
                subtitle = "允许通过快捷方式手动触发打卡",
                showToggle = true,
                toggleOn = viewModel.proactiveToggle,
                onToggle = {
                    viewModel.proactiveToggle = it
                    prefs.edit().putBoolean("proactive_enabled", it).apply()
                    if (it) {
                        ProactiveTriggerReceiver.scheduleAll(context)
                    } else {
                        ProactiveTriggerReceiver.cancelAll(context)
                    }
                },
                isFirst = true,
                showDivider = false,
                isLast = true
            )
        }

        IosSection(
            title = "系统权限",
            footer = if (allGranted) "所有权限均已授权，服务可正常运行。"
                     else "部分权限未授权，可能影响打卡可靠性。点击相应对其授权。"
        ) {
            IosRow(
                icon = IconSms,
                title = "短信读取权限",
                subtitle = "用于监听验证码类打卡短信",
                iconDim = true,
                value = if (smsGranted) "已授权" else "未授权",
                onClick = { onOpenPermissionDetail("sms") }
            )
            IosRow(
                icon = IconBell,
                title = "通知监听权限",
                subtitle = "用于监听钉钉打卡提醒通知",
                iconDim = true,
                value = if (notifGranted) "已授权" else "未授权",
                onClick = { onOpenPermissionDetail("notification") }
            )
            IosRow(
                icon = IconBattery,
                title = "电池优化白名单",
                subtitle = "防止系统在后台关闭监听服务",
                iconDim = true,
                value = if (batteryOptimized) "已优化" else "未优化",
                onClick = { onOpenPermissionDetail("battery") }
            )
            IosRow(
                icon = IconOverlay,
                title = "悬浮窗权限",
                subtitle = "用于快捷触发面板的显示",
                iconDim = true,
                value = if (overlayGranted) "已授权" else "未授权",
                isLast = true,
                onClick = { onOpenPermissionDetail("overlay") }
            )
        }

        IosSection("关于") {
            IosRow(
                icon = IconInfo,
                title = "应用版本",
                value = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
                    } catch (_: Exception) { "未知" }
                },
                showChevron = true,
                isFirst = true,
                isLast = true,
                onClick = onNavigateToAbout
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
