package io.github.nku100.webui.ui.util

import androidx.compose.runtime.Composable

/**
 * Ported from KernelSU: DeferredContent.kt
 *
 * Returns true only after the navigation transition animation has completed.
 * On wasmJs, returns true immediately (no NavDisplay host context available).
 */
@Composable
expect fun rememberContentReady(): Boolean
