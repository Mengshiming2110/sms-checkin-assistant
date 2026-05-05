package com.pengxh.smscheckin.ui.theme

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconFlash: ImageVector
    get() {
        if (_iconFlash != null) return _iconFlash!!
        _iconFlash = ImageVector.Builder("IconFlash", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 2f)
                lineTo(4f, 14f)
                horizontalLineToRelative(5f)
                lineTo(8f, 22f)
                lineTo(17f, 10f)
                horizontalLineToRelative(-5f)
                lineTo(13f, 2f)
                close()
            }
        }.build()
        return _iconFlash!!
    }
private var _iconFlash: ImageVector? = null

val IconSearch: ImageVector
    get() {
        if (_iconSearch != null) return _iconSearch!!
        _iconSearch = ImageVector.Builder("IconSearch", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 4f)
                arcTo(7f, 7f, 0f, false, false, 4f, 11f)
                arcTo(7f, 7f, 0f, false, false, 11f, 18f)
                arcTo(7f, 7f, 0f, false, false, 18f, 11f)
                arcTo(7f, 7f, 0f, false, false, 11f, 4f)
                close()
                moveTo(21f, 21f)
                lineToRelative(-4.3f, -4.3f)
            }
        }.build()
        return _iconSearch!!
    }
private var _iconSearch: ImageVector? = null

val IconBlock: ImageVector
    get() {
        if (_iconBlock != null) return _iconBlock!!
        _iconBlock = ImageVector.Builder("IconBlock", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 21f)
                lineTo(3f, 21f)
                close()
                moveTo(9f, 12f)
                lineTo(15f, 12f)
            }
        }.build()
        return _iconBlock!!
    }
private var _iconBlock: ImageVector? = null

val IconClock: ImageVector
    get() {
        if (_iconClock != null) return _iconClock!!
        _iconClock = ImageVector.Builder("IconClock", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Circle outline - two half arcs
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, false, true, 12f, 21f)
                arcTo(9f, 9f, 0f, false, true, 12f, 3f)
                close()
                // Hands from center
                moveTo(12f, 12f)
                lineTo(12f, 7f)
                moveTo(12f, 12f)
                lineTo(16f, 12f)
            }
        }.build()
        return _iconClock!!
    }
private var _iconClock: ImageVector? = null

val IconOverlay: ImageVector
    get() {
        if (_iconOverlay != null) return _iconOverlay!!
        _iconOverlay = ImageVector.Builder("IconOverlay", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 21f)
                lineTo(3f, 21f)
                close()
                moveTo(7f, 7f)
                lineTo(17f, 7f)
                lineTo(17f, 17f)
                lineTo(7f, 17f)
                close()
            }
        }.build()
        return _iconOverlay!!
    }
private var _iconOverlay: ImageVector? = null

val IconTimer: ImageVector
    get() {
        if (_iconTimer != null) return _iconTimer!!
        _iconTimer = ImageVector.Builder("IconTimer", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Circle outline
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, false, true, 12f, 21f)
                arcTo(9f, 9f, 0f, false, true, 12f, 3f)
                close()
                // Hands
                moveTo(12f, 12f)
                lineTo(12f, 8f)
                moveTo(12f, 12f)
                lineTo(15.5f, 14f)
            }
        }.build()
        return _iconTimer!!
    }
private var _iconTimer: ImageVector? = null

val IconBell: ImageVector
    get() {
        if (_iconBell != null) return _iconBell!!
        _iconBell = ImageVector.Builder("IconBell", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                curveTo(18f, 4.7f, 15.3f, 2f, 12f, 2f)
                curveTo(8.7f, 2f, 6f, 4.7f, 6f, 8f)
                curveTo(6f, 14f, 3f, 16f, 3f, 17f)
                lineTo(21f, 17f)
                curveTo(21f, 16f, 18f, 14f, 18f, 8f)
                close()
                moveTo(13.7f, 21f)
                curveTo(13.1f, 22.1f, 10.9f, 22.1f, 10.3f, 21f)
            }
        }.build()
        return _iconBell!!
    }
private var _iconBell: ImageVector? = null

val IconSms: ImageVector
    get() {
        if (_iconSms != null) return _iconSms!!
        _iconSms = ImageVector.Builder("IconSms", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 21f)
                lineTo(3f, 21f)
                close()
                moveTo(8f, 12f)
                lineTo(16f, 12f)
                moveTo(12f, 8f)
                lineTo(12f, 16f)
            }
        }.build()
        return _iconSms!!
    }
private var _iconSms: ImageVector? = null

val IconDelay: ImageVector
    get() {
        if (_iconDelay != null) return _iconDelay!!
        _iconDelay = ImageVector.Builder("IconDelay", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f); lineTo(12f, 6f)
                moveTo(12f, 18f); lineTo(12f, 22f)
                moveTo(4.9f, 4.9f); lineTo(7.7f, 7.7f)
                moveTo(16.3f, 16.3f); lineTo(19.1f, 19.1f)
                moveTo(2f, 12f); lineTo(6f, 12f)
                moveTo(18f, 12f); lineTo(22f, 12f)
                moveTo(4.9f, 19.1f); lineTo(7.7f, 16.3f)
                moveTo(16.3f, 7.7f); lineTo(19.1f, 4.9f)
            }
        }.build()
        return _iconDelay!!
    }
private var _iconDelay: ImageVector? = null

val IconBattery: ImageVector
    get() {
        if (_iconBattery != null) return _iconBattery!!
        _iconBattery = ImageVector.Builder("IconBattery", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 6f)
                lineTo(22f, 6f)
                lineTo(22f, 18f)
                lineTo(2f, 18f)
                close()
                moveTo(6f, 10f)
                lineTo(18f, 10f)
            }
        }.build()
        return _iconBattery!!
    }
private var _iconBattery: ImageVector? = null

val IconChevronLeft: ImageVector
    get() {
        if (_iconChevronLeft != null) return _iconChevronLeft!!
        _iconChevronLeft = ImageVector.Builder("IconChevronLeft", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 19f)
                lineTo(8f, 12f)
                lineTo(15f, 5f)
            }
        }.build()
        return _iconChevronLeft!!
    }
private var _iconChevronLeft: ImageVector? = null

val IconChevronRight: ImageVector
    get() {
        if (_iconChevronRight != null) return _iconChevronRight!!
        _iconChevronRight = ImageVector.Builder("IconChevronRight", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 6f)
            }
        }.build()
        return _iconChevronRight!!
    }
private var _iconChevronRight: ImageVector? = null

val IconSettings: ImageVector
    get() {
        if (_iconSettings != null) return _iconSettings!!
        _iconSettings = ImageVector.Builder("IconSettings", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                arcTo(3f, 3f, 0f, false, true, 12f, 9f)
                arcTo(3f, 3f, 0f, false, true, 12f, 15f)
                close()
                // Gear teeth
                moveTo(19.4f, 15f)
                lineTo(19.7f, 16.8f)
                lineTo(18.1f, 18.4f)
                lineTo(16.3f, 18.1f)
                lineTo(15.1f, 19.9f)
                lineTo(15.1f, 21f)
                lineTo(8.9f, 21f)
                lineTo(8.9f, 19.9f)
                lineTo(7.7f, 18.1f)
                lineTo(5.9f, 18.4f)
                lineTo(4.3f, 16.8f)
                lineTo(4.6f, 15f)
                lineTo(3f, 14f)
                lineTo(3f, 10f)
                lineTo(4.6f, 9f)
                lineTo(4.3f, 7.2f)
                lineTo(5.9f, 5.6f)
                lineTo(7.7f, 5.9f)
                lineTo(8.9f, 4.1f)
                lineTo(8.9f, 3f)
                lineTo(15.1f, 3f)
                lineTo(15.1f, 4.1f)
                lineTo(16.3f, 5.9f)
                lineTo(18.1f, 5.6f)
                lineTo(19.7f, 7.2f)
                lineTo(19.4f, 9f)
                lineTo(21f, 10f)
                lineTo(21f, 14f)
                close()
            }
        }.build()
        return _iconSettings!!
    }
private var _iconSettings: ImageVector? = null

val IconDashboard: ImageVector
    get() {
        if (_iconDashboard != null) return _iconDashboard!!
        _iconDashboard = ImageVector.Builder("IconDashboard", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f); lineTo(10f, 3f); lineTo(10f, 11f); lineTo(3f, 11f); close()
                moveTo(14f, 3f); lineTo(21f, 3f); lineTo(21f, 11f); lineTo(14f, 11f); close()
                moveTo(3f, 14f); lineTo(10f, 14f); lineTo(10f, 21f); lineTo(3f, 21f); close()
                moveTo(14f, 14f); lineTo(21f, 14f); lineTo(21f, 21f); lineTo(14f, 21f); close()
            }
        }.build()
        return _iconDashboard!!
    }
private var _iconDashboard: ImageVector? = null

val IconInfo: ImageVector
    get() {
        if (_iconInfo != null) return _iconInfo!!
        _iconInfo = ImageVector.Builder("IconInfo", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                arcTo(9f, 9f, 0f, false, true, 21f, 12f)
                arcTo(9f, 9f, 0f, false, true, 12f, 3f)
                arcTo(9f, 9f, 0f, false, true, 3f, 12f)
                arcTo(9f, 9f, 0f, false, true, 12f, 21f)
                close()
                moveTo(12f, 16f)
                lineTo(12f, 12f)
                moveTo(12f, 8f)
                lineTo(12.01f, 8f)
            }
        }.build()
        return _iconInfo!!
    }
private var _iconInfo: ImageVector? = null
