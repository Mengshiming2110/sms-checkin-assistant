package com.pengxh.smscheckin.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pengxh.smscheckin.*
import com.pengxh.smscheckin.ui.components.*
import com.pengxh.smscheckin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: AppViewModel = viewModel(),
    onShowOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE) }
    val isOn = viewModel.serviceEnabled

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableService(context, viewModel)
        else viewModel.serviceEnabled = false
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) checkNotificationAndEnable(context, viewModel, notificationPermissionLauncher)
        else viewModel.serviceEnabled = false
    }

    LaunchedEffect(isOn) {
        while (true) {
            viewModel.refreshStats(context)
            delay(5000)
        }
    }

    // Chain system permission requests (overlay → battery) as user returns from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingSystemPerms) {
                pendingSystemPerms = false
                ensureSystemPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // Subtle accent glow at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.radialGradient(
                        0.0f to AccentSoft,
                        0.7f to Color.Transparent,
                        radius = with(density) { 280.dp.toPx() }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(isOn)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        viewModel.dateString,
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
                Text(
                    "短信打卡助手",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).sp,
                    color = Fg
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Surface)
                    .border(1.dp, Border, CircleShape)
                    .clickable { onShowOnboarding() },
                contentAlignment = Alignment.Center
            ) {
                Text("?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Muted)
            }
        }

        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SwitchRing(
                isOn = isOn,
                onClick = {
                    if (isOn) {
                        disableService(context, viewModel)
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                        } else {
                            checkNotificationAndEnable(context, viewModel, notificationPermissionLauncher)
                        }
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            Text(
                if (isOn) "服务运行中" else "服务未开启",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOn) Fg else Muted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                viewModel.uptimeText,
                fontSize = 13.sp,
                color = Muted
            )
        }

        Spacer(Modifier.height(36.dp))

        StatsStrip(
            todayCount = viewModel.todayCount,
            lastTrigger = viewModel.lastTrigger,
            statusText = viewModel.statusText,
            isOn = isOn,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .animateContentSize()
        ) {
            Text(
                "触发记录",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.04.sp,
                color = if (isOn) Accent else Disabled
            )
            Spacer(Modifier.height(12.dp))

            if (!isOn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.45f)
                        .background(SurfaceOff, RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("服务未开启，暂无触发记录", fontSize = 13.sp, color = Disabled)
                }
            } else if (viewModel.logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceOff, RoundedCornerShape(8.dp))
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无触发记录", fontSize = 13.sp, color = Disabled)
                }
            } else {
                viewModel.logs.forEachIndexed { index, log ->
                    LogCard(
                        time = log.time,
                        category = log.category,
                        preview = log.preview,
                        onClick = { viewModel.selectedLogIndex = index },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        }
    }

    viewModel.selectedLogIndex?.let { index ->
        val log = viewModel.logs.getOrNull(index)
        if (log != null) {
            LogDetailModal(
                badgeCategory = log.category,
                badgeText = log.badgeText,
                time = log.time,
                content = log.fullContent,
                meta = log.meta,
                onDismiss = { viewModel.selectedLogIndex = null }
            )
        }
    }
}

private fun checkNotificationAndEnable(
    context: Context,
    viewModel: AppViewModel,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableService(context, viewModel)
        }
    } else {
        enableService(context, viewModel)
    }
}

private fun enableService(context: Context, viewModel: AppViewModel) {
    SmsReceiver.isEnabled = true
    context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("service_enabled", true)
        .putLong("service_start_time", System.currentTimeMillis())
        .apply()
    context.startForegroundService(Intent(context, CheckInForegroundService::class.java))
    DailyReportReceiver.scheduleDailyReport(context)
    KeepAliveWorker.schedule(context)
    MissedCheckInReceiver.schedule(context)
    ProactiveTriggerReceiver.scheduleAll(context)
    viewModel.serviceEnabled = true
    viewModel.refreshStats(context)
    ensureSystemPermissions(context)
}

private fun disableService(context: Context, viewModel: AppViewModel) {
    SmsReceiver.isEnabled = false
    context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("service_enabled", false)
        .remove("service_start_time")
        .apply()
    context.stopService(Intent(context, CheckInForegroundService::class.java))
    DailyReportReceiver.cancelDailyReport(context)
    KeepAliveWorker.cancel(context)
    MissedCheckInReceiver.cancel(context)
    ProactiveTriggerReceiver.cancelAll(context)
    viewModel.serviceEnabled = false
    viewModel.refreshStats(context)
}

private fun ensureSystemPermissions(context: Context) {
    // 1. Notification listener — needed for WeChat/DingTalk notification monitoring
    try {
        val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        if (!listeners.contains(context.packageName)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                pendingSystemPerms = true
                return
            }
        }
    } catch (_: Exception) {
        // Some OEM ROMs block access to this setting — skip gracefully
    }

    // 2. Overlay — needed for opening DingTalk from background
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                pendingSystemPerms = true
                return
            }
        } catch (_: Exception) { }
    }

    // 3. Battery optimization — prevent system from killing service
    checkBatteryOptimization(context)
}

private fun checkBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            pendingSystemPerms = true
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}

internal var pendingSystemPerms = false
