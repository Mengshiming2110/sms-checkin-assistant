package com.pengxh.smscheckin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pengxh.smscheckin.ui.theme.*

@Composable
fun StatusDot(isOn: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        if (isOn) Ok else Disabled,
        animationSpec = tween(400)
    )
    Box(
        modifier = modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun SwitchRing(
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    var showIconPop by remember { mutableStateOf(false) }
    var showGlow by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val iconScale by animateFloatAsState(
        if (showIconPop) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    LaunchedEffect(isOn) {
        if (isOn) {
            showIconPop = true
            showGlow = true
            kotlinx.coroutines.delay(700)
            showGlow = false
            showIconPop = false
        }
    }

    val bgColor by animateColorAsState(
        if (isOn) Accent else Disabled,
        animationSpec = tween(350)
    )

    Box(
        modifier = modifier
            .size(126.dp)
            .scale(scaleAnim)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (showGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.4f
                        scaleY = 1.4f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            0.3f to Accent.copy(alpha = 0.25f),
                            0.7f to Accent.copy(alpha = 0.08f),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .size(126.dp)
                .shadow(
                    elevation = if (isOn) 16.dp else 0.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = if (isOn) Accent.copy(alpha = 0.30f) else Color.Transparent,
                    spotColor = if (isOn) Accent.copy(alpha = 0.30f) else Color.Transparent
                )
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconFlash,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(38.dp)
                    .scale(iconScale)
            )
        }
    }
}

@Composable
fun StatsStrip(
    todayCount: String,
    lastTrigger: String,
    statusText: String,
    isOn: Boolean,
    modifier: Modifier = Modifier
) {
    val stripAlpha by animateFloatAsState(
        if (isOn) 1f else 0.4f,
        animationSpec = tween(400)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(stripAlpha)
            .background(Surface, RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatColumn(
            value = todayCount,
            label = "今日触发",
            valueColor = if (isOn && todayCount != "—") Accent else Muted,
            modifier = Modifier.weight(1f)
        )
        StatDivider(height = 36.dp)
        StatColumn(
            value = lastTrigger,
            label = "最后触发",
            valueFontSize = if (isOn && lastTrigger != "—:—") 22.sp else 14.sp,
            valueColor = if (isOn && lastTrigger != "—:—") Accent else Muted,
            modifier = Modifier.weight(1f)
        )
        StatDivider(height = 36.dp)
        StatColumn(
            value = statusText,
            label = "运行状态",
            valueFontSize = if (isOn) 22.sp else 16.sp,
            valueColor = if (isOn) Accent else Muted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    valueFontSize: androidx.compose.ui.unit.TextUnit = 36.sp,
    valueColor: Color = Accent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.03).sp,
            color = valueColor
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Muted
        )
    }
}

@Composable
fun StatDivider(height: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(height)
            .background(Border)
    )
}

@Composable
fun IosToggle(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        if (isOn) Ok else Border,
        animationSpec = tween(200)
    )
    val offset by animateDpAsState(
        if (isOn) 20.dp else 0.dp,
        animationSpec = tween(200)
    )

    Box(
        modifier = modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle() }
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset, y = 2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
        )
    }
}

@Composable
fun LogBadge(category: String, modifier: Modifier = Modifier) {
    val (bg, textColor) = when (category) {
        "sms" -> Accent.copy(alpha = 0.10f) to Accent
        "notif" -> Ok.copy(alpha = 0.10f) to Ok
        else -> Color.Black.copy(alpha = 0.05f) to Muted
    }
    val label = when (category) {
        "sms" -> "短信通知"
        "notif" -> "通知监听"
        else -> "主动触发"
    }

    Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.03.sp,
        color = textColor,
        modifier = modifier
            .background(bg, RoundedCornerShape(9999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun LogCard(
    time: String,
    category: String,
    preview: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.width(58.dp)
        )
        Spacer(Modifier.width(4.dp))
        LogBadge(category)
        Spacer(Modifier.width(6.dp))
        Text(
            text = preview,
            fontSize = 13.sp,
            color = Fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun IosNavBar(
    title: String,
    onBack: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Surface.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable { onBack() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = IconChevronLeft,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "设置",
                fontSize = 16.sp,
                color = Accent
            )
        }
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Fg,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                fontSize = 16.sp,
                color = Accent,
                modifier = Modifier
                    .clickable { onAction() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        } else {
            Spacer(Modifier.width(44.dp))
        }
    }
}

@Composable
fun IosSection(
    title: String,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.padding(start = 20.dp, bottom = 6.dp, top = 8.dp)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(Surface, RoundedCornerShape(12.dp))
        ) {
            content()
        }
        if (footer != null) {
            Text(
                footer,
                fontSize = 12.sp,
                color = Muted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp)
            )
        }
    }
}

@Composable
fun IosRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    iconDim: Boolean = false,
    showChevron: Boolean = false,
    showToggle: Boolean = false,
    toggleOn: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable { onClick() }
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (iconDim) Muted else Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = Fg)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 1.dp))
                }
            }
            if (value != null) {
                Text(
                    value,
                    fontSize = 16.sp,
                    color = Muted,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
            if (showChevron) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconChevronRight,
                        contentDescription = null,
                        tint = Disabled,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            if (showToggle && onToggle != null) {
                Spacer(Modifier.width(8.dp))
                IosToggle(isOn = toggleOn, onToggle = { onToggle(!toggleOn) })
            }
        }
        if (!isLast && showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp)
                    .height(1.dp)
                    .background(Border)
            )
        }
    }
}

@Composable
fun IosSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: IntRange = 0..30,
    valueLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val min = valueRange.first
    val max = valueRange.last

    Column(modifier = modifier) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconDelay,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("触发延迟", fontSize = 16.sp, color = Fg, modifier = Modifier.weight(1f))
            Text("$value 秒", fontSize = 16.sp, color = Fg)
        }

        // Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$min", fontSize = 12.sp, color = Muted)
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = min.toFloat()..max.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Color(0xFFE8E8ED),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            Text("$max", fontSize = 12.sp, color = Muted)
        }

        Spacer(Modifier.height(6.dp))
    }
}
