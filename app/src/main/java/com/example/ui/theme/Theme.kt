package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppUiTheme = staticCompositionLocalOf { AppUiTheme.OBSIDIAN_GOLD }

@Composable
fun LibreChessTheme(
    appTheme: AppUiTheme = AppUiTheme.OBSIDIAN_GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = appTheme.primary,
        onPrimary = appTheme.onPrimary,
        primaryContainer = appTheme.surfaceVariant,
        onPrimaryContainer = appTheme.textPrimary,
        secondary = appTheme.secondary,
        onSecondary = appTheme.background,
        tertiary = appTheme.tertiary,
        background = appTheme.background,
        onBackground = appTheme.textPrimary,
        surface = appTheme.surface,
        onSurface = appTheme.textPrimary,
        surfaceVariant = appTheme.surfaceVariant,
        onSurfaceVariant = appTheme.textSecondary,
        outline = appTheme.cardBorder
    )

    CompositionLocalProvider(LocalAppUiTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    LibreChessTheme(content = content)
}
