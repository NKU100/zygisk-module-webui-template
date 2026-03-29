package io.github.nku100.webui.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
actual fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    // wasmJs has no system dark mode detection, default to dark
    val isDark = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> true
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val controller = ThemeController(
        colorSchemeMode = if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight,
        isDark = isDark,
    )

    MiuixTheme(
        controller = controller,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    )
}
