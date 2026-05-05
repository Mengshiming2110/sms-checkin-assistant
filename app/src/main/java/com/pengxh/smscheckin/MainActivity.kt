package com.pengxh.smscheckin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pengxh.smscheckin.ui.components.OnboardingModal
import com.pengxh.smscheckin.ui.components.PermissionDetailModal
import com.pengxh.smscheckin.ui.components.TimePickerSheet
import com.pengxh.smscheckin.ui.components.UpdateDialog
import com.pengxh.smscheckin.ui.screens.*
import com.pengxh.smscheckin.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmsCheckInTheme {
                MainApp()
            }
        }
    }
}

@Composable
private fun MainApp() {
    val viewModel: AppViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var navStack by remember { mutableStateOf<List<String>>(listOf("dashboard")) }

    var showOnboarding by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf<String?>(null) }
    var showPermDetail by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarded", false)) {
            kotlinx.coroutines.delay(500)
            showOnboarding = true
            prefs.edit().putBoolean("onboarded", true).apply()
        }
    }

    // Auto-check for update on startup
    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(2000)
            UpdateChecker.checkForUpdate(context) { result ->
                result.onSuccess { info ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        pendingUpdate = info
                        showUpdateDialog = true
                    }
                }
            }
        } catch (_: Exception) {
            // Silently ignore update check failures on startup
        }
    }

    val isSubPage = navStack.last() != "dashboard" && navStack.last() != "settings"

    Scaffold(
        bottomBar = {
            if (!isSubPage) {
                BottomTabBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                        navStack = listOf(if (tab == 0) "dashboard" else "settings")
                    }
                )
            }
        },
        containerColor = Bg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            when (navStack.last()) {
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    onShowOnboarding = { showOnboarding = true }
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToKeywords = { navStack = navStack + "keywords" },
                    onNavigateToWhitelist = { navStack = navStack + "whitelist" },
                    onOpenTimePicker = { showTimePicker = it },
                    onOpenPermissionDetail = { showPermDetail = it },
                    onNavigateToAbout = { navStack = navStack + "about" }
                )
                "keywords" -> KeywordsScreen(
                    viewModel = viewModel,
                    onBack = { navStack = navStack.dropLast(1) }
                )
                "whitelist" -> WhitelistScreen(
                    viewModel = viewModel,
                    onBack = { navStack = navStack.dropLast(1) }
                )
                "about" -> AboutScreen(
                    onBack = { navStack = navStack.dropLast(1) }
                )
            }
        }
    }

    if (showOnboarding) {
        OnboardingModal(onDismiss = { showOnboarding = false })
    }

    showTimePicker?.let { target ->
        val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
        val isWork = target == "work-start"
        val title = if (isWork) "上班打卡时间" else "下班打卡时间"
        TimePickerSheet(
            title = title,
            startHour = if (isWork) viewModel.workStartHour else viewModel.offWorkStartHour,
            startMin = if (isWork) viewModel.workStartMin else viewModel.offWorkStartMin,
            endHour = if (isWork) viewModel.workEndHour else viewModel.offWorkEndHour,
            endMin = if (isWork) viewModel.workEndMin else viewModel.offWorkEndMin,
            onCancel = { showTimePicker = null },
            onConfirm = { sh, sm, eh, em ->
                val startKey = if (isWork) "window_morning_start" else "window_evening_start"
                val endKey = if (isWork) "window_morning_end" else "window_evening_end"
                prefs.edit()
                    .putString(startKey, "$sh:$sm")
                    .putString(endKey, "$eh:$em")
                    .apply()
                if (isWork) {
                    viewModel.workStartHour = sh; viewModel.workStartMin = sm
                    viewModel.workEndHour = eh; viewModel.workEndMin = em
                } else {
                    viewModel.offWorkStartHour = sh; viewModel.offWorkStartMin = sm
                    viewModel.offWorkEndHour = eh; viewModel.offWorkEndMin = em
                }
                SmsReceiver.notifyConfigChanged()
                showTimePicker = null
            }
        )
    }

    showPermDetail?.let { key ->
        val (title, desc, status) = when (key) {
            "sms" -> Triple("短信读取权限", "Android 系统权限，允许应用读取收到的短信内容。本应用仅读取短信中的关键字以判定是否需要触发打卡，不会上传或存储任何短信内容。", "已授权")
            "notification" -> Triple("通知监听权限", "Android 系统权限，允许应用监听其他应用发出的通知内容。本应用仅监听钉钉等白名单应用的通知，匹配关键字后触发打卡。", "已授权")
            "battery" -> Triple("电池优化白名单", "将本应用加入系统电池优化白名单，防止 Android 在后台因省电策略关闭监听服务。建议始终开启以保证打卡可靠性。", "已加入白名单")
            else -> Triple("悬浮窗权限", "允许应用在其他应用上方显示悬浮窗。本应用使用此权限提供快捷触发面板，方便你随时手动触发打卡。", "已授权")
        }
        PermissionDetailModal(
            title = title,
            description = desc,
            status = status,
            onDismiss = { showPermDetail = null }
        )
    }

    if (showUpdateDialog && pendingUpdate != null) {
        UpdateDialog(
            updateInfo = pendingUpdate!!,
            onDismiss = {
                showUpdateDialog = false
                pendingUpdate = null
            },
            onDownload = {
                UpdateChecker.downloadAndInstall(context, pendingUpdate!!.downloadUrl)
                showUpdateDialog = false
                pendingUpdate = null
            }
        )
    }
}

@Composable
private fun BottomTabBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bg.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem(
                icon = IconDashboard,
                text = "仪表盘",
                selected = currentTab == 0,
                onClick = { onTabSelected(0) }
            )
            TabItem(
                icon = IconSettings,
                text = "设置",
                selected = currentTab == 1,
                onClick = { onTabSelected(1) }
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        if (selected) Accent else Muted,
        animationSpec = tween(180)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.03.sp,
            color = color
        )
    }
}
