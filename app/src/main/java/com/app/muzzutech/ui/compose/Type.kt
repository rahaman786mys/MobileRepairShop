package com.app.muzzutech.ui.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val default = Typography()

val MuzzuTypography = Typography(
    headlineLarge = default.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = default.bodyLarge.copy(fontSize = 15.sp),
    bodyMedium = default.bodyMedium.copy(fontSize = 13.sp),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = default.labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
    labelSmall = default.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    )
)
