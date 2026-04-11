package io.github.nku100.webui.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nku100.webui.platform.isAndroidPlatform
import io.github.nku100.webui.platform.statusBarTopPadding

/**
 * Returns the status bar top padding that should be applied manually to the TopAppBar.
 *
 * On Android, the miuix TopAppBar handles status bar insets via WindowInsets.systemBars,
 * so this returns 0.dp.
 * On wasmJs (KernelSU WebView), WindowInsets.systemBars returns 0 because Compose for Web
 * has no native insets support. This returns the actual status bar height read from
 * the --safe-area-inset-top CSS variable injected by KernelSU after enableEdgeToEdge(true).
 */
@Composable
fun wasmStatusBarPadding(): Dp =
    if (!isAndroidPlatform) statusBarTopPadding() else 0.dp
