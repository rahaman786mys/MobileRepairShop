package com.app.muzzutech.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = MuzzuAccent,
    onPrimary = Color.White,
    secondary = MuzzuPrimary,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = LightTextMain,
    surface = LightSurface,
    onSurface = LightTextMain,
    surfaceVariant = LightBg,
    onSurfaceVariant = LightTextSub,
    outline = LightBorder,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = MuzzuAccent,
    onPrimary = Color.White,
    secondary = MuzzuPrimary,
    onSecondary = Color.White,
    background = DarkBg,
    onBackground = DarkTextMain,
    surface = DarkSurface,
    onSurface = DarkTextMain,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSub,
    outline = DarkBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MuzzuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(Unit) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MuzzuTypography,
        content = content
    )
}
