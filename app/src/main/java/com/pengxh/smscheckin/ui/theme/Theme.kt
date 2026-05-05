package com.pengxh.smscheckin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

@Composable
fun SmsCheckInTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()

    SideEffect { setDarkTheme(dark) }

    val scheme = if (dark) {
        darkColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            primaryContainer = AccentSoft,
            secondary = Ok,
            background = Bg,
            surface = Surface,
            onBackground = Fg,
            onSurface = Fg,
            outline = Border,
            outlineVariant = Disabled,
            surfaceVariant = SurfaceOff
        )
    } else {
        lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            primaryContainer = AccentSoft,
            secondary = Ok,
            background = Bg,
            surface = Surface,
            onBackground = Fg,
            onSurface = Fg,
            outline = Border,
            outlineVariant = Disabled,
            surfaceVariant = SurfaceOff
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}
