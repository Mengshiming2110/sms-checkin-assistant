package com.pengxh.smscheckin.ui.theme

import androidx.compose.ui.graphics.Color

private var isDark = false

val Accent: Color get() = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
val AccentDeep: Color get() = if (isDark) Color(0xFF409CFF) else Color(0xFF0056CC)
val AccentSoft: Color get() = if (isDark) Color(0x1A0A84FF) else Color(0x14007AFF)
val Bg: Color get() = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
val Surface: Color get() = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
val SurfaceOff: Color get() = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
val Fg: Color get() = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
val Muted: Color get() = if (isDark) Color(0xFF98989D) else Color(0xFF8E8E93)
val Border: Color get() = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
val Disabled: Color get() = if (isDark) Color(0xFF636366) else Color(0xFFC7C7CC)
val Ok: Color get() = if (isDark) Color(0xFF30D158) else Color(0xFF34C759)
val Red: Color get() = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30)
val SurfaceTinted: Color get() = if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000)

internal fun setDarkTheme(dark: Boolean) { isDark = dark }
