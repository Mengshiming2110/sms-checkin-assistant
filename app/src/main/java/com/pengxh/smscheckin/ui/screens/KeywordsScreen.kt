package com.pengxh.smscheckin.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pengxh.smscheckin.SmsReceiver
import com.pengxh.smscheckin.ui.components.IosNavBar
import com.pengxh.smscheckin.ui.components.KeywordEditorSheet
import com.pengxh.smscheckin.ui.theme.*

@Composable
fun KeywordsScreen(
    viewModel: AppViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        IosNavBar(
            title = "触发关键字",
            onBack = onBack,
            actionLabel = "添加",
            onAction = {
                editingIndex = -1
                showEditor = true
            }
        )

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (viewModel.keywords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无关键字\n点击右上角「添加」创建第一个触发关键字",
                        fontSize = 14.sp,
                        color = Muted
                    )
                }
            } else {
                viewModel.keywords.forEachIndexed { index, keyword ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Surface,
                                    when {
                                        index == 0 && viewModel.keywords.size == 1 -> RoundedCornerShape(12.dp)
                                        index == 0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                        index == viewModel.keywords.size - 1 -> RoundedCornerShape(
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp
                                        )
                                        else -> RoundedCornerShape(0.dp)
                                    }
                                )
                                .clickable {
                                    editingIndex = index
                                    showEditor = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                keyword,
                                fontSize = 16.sp,
                                color = Fg,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Red)
                                    .clickable {
                                        viewModel.keywords.removeAt(index)
                                        saveKeywords(context, viewModel)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("×", fontSize = 16.sp, color = Color.White)
                            }
                        }
                        if (index < viewModel.keywords.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(Border)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        KeywordEditorSheet(
            isEditing = editingIndex >= 0,
            initialValue = if (editingIndex >= 0) viewModel.keywords.getOrElse(editingIndex) { "" } else "",
            onCancel = {
                showEditor = false
                editingIndex = -1
            },
            onSave = { value ->
                if (value.isNotEmpty()) {
                    if (editingIndex >= 0) {
                        viewModel.keywords[editingIndex] = value
                    } else {
                        if (!viewModel.keywords.contains(value)) {
                            viewModel.keywords.add(value)
                        }
                    }
                    saveKeywords(context, viewModel)
                }
                showEditor = false
                editingIndex = -1
            }
        )
    }
}

private fun saveKeywords(context: Context, viewModel: AppViewModel) {
    val prefs = context.getSharedPreferences("sms_checkin_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("keywords", SmsReceiver.keywordsToJson(viewModel.keywords.toList())).apply()
    SmsReceiver.keywords = viewModel.keywords.toList()
    SmsReceiver.notifyConfigChanged()
}
