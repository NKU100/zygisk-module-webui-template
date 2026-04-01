package io.github.nku100.webui.ui.theme

import androidx.compose.runtime.Composable

@JsFun("() => window.matchMedia('(prefers-color-scheme: dark)').matches")
private external fun isBrowserDarkMode(): Boolean

@Composable
actual fun isSystemDarkTheme(): Boolean = isBrowserDarkMode()
