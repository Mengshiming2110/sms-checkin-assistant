package com.pengxh.smscheckin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pengxh.smscheckin.UpdateChecker
import com.pengxh.smscheckin.ui.theme.*

@Composable
fun OnboardingModal(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp, 28.dp, 28.dp, 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "欢迎使用短信打卡助手",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg
                )
                Spacer(Modifier.height(22.dp))

                OnboardStep(1, "前置条件", "请确认钉钉已开启极速打卡功能，并保持后台运行。", showDivider = true)
                OnboardStep(2, "必须权限", "短信读取权限 · 通知监听权限。两项均为系统级授权，仅用于匹配关键字触发打卡。", showDivider = true)
                OnboardStep(3, "使用方法", "安装授权 → 配置触发关键字 → 开启服务开关 → 设好即忘。", showDivider = false)

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "知道了",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.02.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardStep(num: Int, title: String, desc: String, showDivider: Boolean) {
    Column {
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Border)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text("$num", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Fg)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 12.sp, color = Muted, lineHeight = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    title: String,
    startHour: String,
    startMin: String,
    endHour: String,
    endMin: String,
    onCancel: () -> Unit,
    onConfirm: (sh: String, sm: String, eh: String, em: String) -> Unit
) {
    var sh by remember { mutableStateOf(startHour) }
    var sm by remember { mutableStateOf(startMin) }
    var eh by remember { mutableStateOf(endHour) }
    var em by remember { mutableStateOf(endMin) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Disabled)
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            TimeInputRow("开始时间", sh, sm,
                onHour = { sh = it.filter { c -> c.isDigit() }.take(2) },
                onMin = { sm = it.filter { c -> c.isDigit() }.take(2) }
            )
            Spacer(Modifier.height(4.dp))
            TimeInputRow("结束时间", eh, em,
                onHour = { eh = it.filter { c -> c.isDigit() }.take(2) },
                onMin = { em = it.filter { c -> c.isDigit() }.take(2) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Border)
                ) {
                    Text("取消", color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { onConfirm(sh, sm, eh, em) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("确定", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TimeInputRow(
    label: String,
    hour: String,
    min: String,
    onHour: (String) -> Unit,
    onMin: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = Fg, modifier = Modifier.width(72.dp))
        OutlinedTextField(
            value = hour,
            onValueChange = onHour,
            modifier = Modifier.width(56.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        Text(
            ":",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        OutlinedTextField(
            value = min,
            onValueChange = onMin,
            modifier = Modifier.width(56.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun LogDetailModal(
    badgeCategory: String,
    badgeText: String,
    time: String,
    content: String,
    meta: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                LogBadge(badgeCategory)
                Spacer(Modifier.height(8.dp))
                Text(time, fontSize = 13.sp, color = Muted)
                Spacer(Modifier.height(8.dp))
                Text(content, fontSize = 15.sp, color = Fg, lineHeight = 24.sp)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                Spacer(Modifier.height(10.dp))
                Text(meta, fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("关闭", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PermissionDetailModal(
    title: String,
    description: String,
    status: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Fg)
                Spacer(Modifier.height(8.dp))
                Text(description, fontSize = 14.sp, color = Muted, lineHeight = 22.sp)
                Spacer(Modifier.height(6.dp))
                Text(status, fontSize = 12.sp, color = Ok)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("知道了", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordEditorSheet(
    isEditing: Boolean,
    initialValue: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Disabled)
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                if (isEditing) "编辑关键字" else "添加关键字",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 20) value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入触发关键字，如：打卡", color = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Border)
                ) {
                    Text("取消", color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { onSave(value.trim()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    enabled = value.isNotBlank()
                ) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsNumberEditorSheet(
    numbers: List<String>,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var items by remember { mutableStateOf(numbers.toMutableList()) }
    var input by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Disabled)
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                "短信白名单号码",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 20) input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入发件人号码", color = Muted, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val v = input.trim()
                        if (v.isNotEmpty() && !items.contains(v)) {
                            items = items.toMutableList().apply { add(v) }
                        }
                        input = ""
                    },
                    enabled = input.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("添加", fontSize = 14.sp)
                }
            }

            if (items.isEmpty()) {
                Text("未设置白名单号码\n所有发件人的短信均会触发",
                    fontSize = 13.sp, color = Muted,
                    modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center)
            } else {
                items.forEachIndexed { i, num ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(num, fontSize = 15.sp, color = Fg, modifier = Modifier.weight(1f))
                        Text("×", fontSize = 18.sp, color = Muted,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable {
                                    items = items.toMutableList().apply { removeAt(i) }
                                }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Border)
                ) {
                    Text("取消", color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {
                        val v = input.trim()
                        if (v.isNotEmpty() && !items.contains(v)) {
                            items = items.toMutableList().apply { add(v) }
                        }
                        onSave(items)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WxContactEditorSheet(
    contacts: List<String>,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var items by remember { mutableStateOf(contacts.toMutableList()) }
    var input by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Disabled)
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                "白名单微信联系人",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 20) input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入联系人昵称或备注", color = Muted, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val v = input.trim()
                        if (v.isNotEmpty() && !items.contains(v)) {
                            items = items.toMutableList().apply { add(v) }
                        }
                        input = ""
                    },
                    enabled = input.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("添加", fontSize = 14.sp)
                }
            }

            if (items.isEmpty()) {
                Text("未设置白名单联系人\n所有联系人的微信消息均会触发",
                    fontSize = 13.sp, color = Muted,
                    modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center)
            } else {
                items.forEachIndexed { i, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontSize = 15.sp, color = Fg, modifier = Modifier.weight(1f))
                        Text("×", fontSize = 18.sp, color = Muted,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable {
                                    items = items.toMutableList().apply { removeAt(i) }
                                }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Border)
                ) {
                    Text("取消", color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {
                        val v = input.trim()
                        if (v.isNotEmpty() && !items.contains(v)) {
                            items = items.toMutableList().apply { add(v) }
                        }
                        onSave(items)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateChecker.UpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "发现新版本 ${updateInfo.version}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Fg
                )
                Spacer(Modifier.height(8.dp))

                val lines = updateInfo.changelog.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    Text("更新内容", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Fg)
                    Spacer(Modifier.height(4.dp))
                    lines.take(10).forEach { line ->
                        Text(
                            "· $line",
                            fontSize = 13.sp,
                            color = Muted,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Border)
                    ) {
                        Text("稍后再说", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Fg)
                    }
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("立即更新", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
