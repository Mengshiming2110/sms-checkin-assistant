package com.pengxh.smscheckin.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pengxh.smscheckin.UpdateChecker
import com.pengxh.smscheckin.ui.components.IosNavBar
import com.pengxh.smscheckin.ui.theme.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (_: Exception) {
            "未知"
        }
    }

    var checking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<Result<UpdateChecker.UpdateInfo>?>(null) }

    fun doCheck() {
        checking = true
        UpdateChecker.checkForUpdate(context) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                updateResult = result
                checking = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        IosNavBar(title = "应用版本", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Current version card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("当前版本", fontSize = 13.sp, color = Muted)
                Spacer(Modifier.height(4.dp))
                Text(currentVersion, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Accent)
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { doCheck() },
                    enabled = !checking,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (checking) "检查中…" else "检查更新",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Result section
            Spacer(Modifier.height(12.dp))
            val result = updateResult
            if (result != null) {
                result.fold(
                    onSuccess = { info ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface, RoundedCornerShape(12.dp))
                                .padding(20.dp)
                        ) {
                            Text(
                                "发现新版本 ${info.version}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Fg
                            )
                            Spacer(Modifier.height(8.dp))

                            val lines = info.changelog.lines().filter { it.isNotBlank() }
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

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    UpdateChecker.downloadAndInstall(context, info.downloadUrl)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("立即更新", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    },
                    onFailure = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .background(Surface, RoundedCornerShape(12.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "已是最新版本",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ok
                            )
                        }
                    }
                )
            }
        }
    }
}
