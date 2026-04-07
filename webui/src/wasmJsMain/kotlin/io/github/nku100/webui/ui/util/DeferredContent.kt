package io.github.nku100.webui.ui.util

import androidx.compose.runtime.Composable

// On wasmJs, NavDisplay host context is not available, so content is always ready.
@Composable
actual fun rememberContentReady(): Boolean = true
