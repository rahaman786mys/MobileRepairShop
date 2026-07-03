package com.app.muzzutech.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = MuzzuAccent,
    onPrimary = LightSurface,
    primaryContainer = MuzzuAccentLight,
    onPrimaryContainer = MuzzuAccentDark,
    secondary = MuzzuPrimary,
    onSecondary = LightSurface,
    background = LightBg,
    onBackground = LightTextMain,
    surface = LightSurface,
    onSurface = LightTextMain,
    surfaceVariant = LightBg,
    onSurfaceVariant = LightTextSub,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = ErrorRed,
    errorContainer = ErrorRed.copy(alpha = 0.12f),
    onErrorContainer = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = MuzzuAccent,
    onPrimary = DarkSurface,
    primaryContainer = MuzzuAccentDark,
    onPrimaryContainer = MuzzuAccentLight,
    secondary = LightSurface,
    onSecondary = MuzzuPrimary,
    background = DarkBg,
    onBackground = DarkTextMain,
    surface = DarkSurface,
    onSurface = DarkTextMain,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSub,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ErrorRed,
    errorContainer = ErrorRed.copy(alpha = 0.18f),
    onErrorContainer = ErrorRed
)

@Composable
fun MuzzuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.statusBarColor = if (darkTheme) DarkBg.toArgb() else MuzzuPrimary.toArgb()
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MuzzuTypography,
        content = content
    )
}
