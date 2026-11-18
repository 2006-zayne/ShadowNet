package com.shadownet.calculator.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = DarkOperatorKeyBg,
    onPrimary = DarkOperatorKeyContent,
    secondary = DarkScientificKeyBg,
    onSecondary = DarkScientificKeyContent,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkNumberKeyContent,
    onSurface = DarkNumberKeyContent
)

private val LightColorScheme = lightColorScheme(
    primary = LightOperatorKeyBg,
    onPrimary = LightOperatorKeyContent,
    secondary = LightScientificKeyBg,
    onSecondary = LightScientificKeyContent,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightNumberKeyContent,
    onSurface = LightNumberKeyContent
)

@Composable
fun CalculatorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
