package io.github.nku100.webui.ui.theme

import androidx.compose.runtime.Composable

/**
 * Theme mode for the app.
 */
enum class ThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Platform-themed root composable.
 * - Android: wraps content in MiuixTheme
 * - wasmJs: wraps content in Material3 theme
 */
@Composable
expect fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
)
