package com.pengxh.smscheckin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SystemFont = FontFamily.Default

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = (-0.03).sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.03.sp,
        lineHeight = 14.sp
    )
)
