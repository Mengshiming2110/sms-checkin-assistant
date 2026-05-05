package com.pengxh.smscheckin.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pengxh.smscheckin.SmsReceiver
import com.pengxh.smscheckin.ui.components.*
import com.pengxh.smscheckin.ui.theme.*

@Composable
fun WhitelistScreen(
    viewModel: AppViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE) }

    val smsEnabled = viewModel.smsEnabled
    val smsNumbers = viewModel.smsNumbers.toList()
    val wechatEnabled = viewModel.wechatEnabled
    val wechatContacts = viewModel.wechatContacts.toList()

    var showSmsEditor by remember { mutableStateOf(false) }
    var showWechatEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        IosNavBar(
            title = "监听来源",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "开启后监听含关键字的通知或短信",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp, top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(Surface, RoundedCornerShape(12.dp))
            ) {
                // SMS row
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (smsEnabled) showSmsEditor = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("短信", fontSize = 16.sp, color = Fg)
                            Text(
                                if (smsNumbers.isEmpty()) "所有号码" else smsNumbers.joinToString("、"),
                                fontSize = 12.sp,
                                color = Muted,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        IosToggle(
                            isOn = smsEnabled,
                            onToggle = {
                                viewModel.smsEnabled = !smsEnabled
                                prefs.edit().putBoolean("sms_enabled", viewModel.smsEnabled).apply()
                                SmsReceiver.smsEnabled = viewModel.smsEnabled
                                SmsReceiver.notifyConfigChanged()
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 60.dp)
                            .height(1.dp)
                            .background(Border)
                    )
                }

                // WeChat row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (wechatEnabled) showWechatEditor = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("微信", fontSize = 16.sp, color = Fg)
                        Text(
                            if (wechatContacts.isEmpty()) "所有联系人" else wechatContacts.joinToString("、"),
                            fontSize = 12.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    IosToggle(
                        isOn = wechatEnabled,
                        onToggle = {
                            viewModel.wechatEnabled = !wechatEnabled
                            prefs.edit().putBoolean("wechat_enabled", viewModel.wechatEnabled).apply()
                            SmsReceiver.wechatEnabled = viewModel.wechatEnabled
                            SmsReceiver.notifyConfigChanged()
                        }
                    )
                }
            }

            Text(
                "至少开启一项来源，打卡监听才能生效。白名单为空时监听对应来源的所有消息。",
                fontSize = 12.sp,
                color = Muted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp)
            )
        }
    }

    if (showSmsEditor) {
        SmsNumberEditorSheet(
            numbers = smsNumbers,
            onCancel = { showSmsEditor = false },
            onSave = { newNumbers ->
                viewModel.smsNumbers.clear()
                viewModel.smsNumbers.addAll(newNumbers)
                prefs.edit()
                    .putString("sms_numbers", SmsReceiver.keywordsToJson(newNumbers))
                    .apply()
                SmsReceiver.smsNumbers.clear()
                SmsReceiver.smsNumbers.addAll(newNumbers)
                SmsReceiver.notifyConfigChanged()
                showSmsEditor = false
            }
        )
    }

    if (showWechatEditor) {
        WxContactEditorSheet(
            contacts = wechatContacts,
            onCancel = { showWechatEditor = false },
            onSave = { newContacts ->
                viewModel.wechatContacts.clear()
                viewModel.wechatContacts.addAll(newContacts)
                prefs.edit()
                    .putString("wechat_contacts", SmsReceiver.keywordsToJson(newContacts))
                    .apply()
                SmsReceiver.wechatContacts.clear()
                SmsReceiver.wechatContacts.addAll(newContacts)
                SmsReceiver.notifyConfigChanged()
                showWechatEditor = false
            }
        )
    }
}
