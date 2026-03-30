package io.github.nku100.webui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
actual fun isSystemDarkTheme(): Boolean = isSystemInDarkTheme()
