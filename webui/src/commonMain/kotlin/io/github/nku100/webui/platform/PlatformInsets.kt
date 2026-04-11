package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Top padding for system status bar.
 * - Android: WindowInsets.statusBars top padding
 * - wasmJs: reads --safe-area-inset-top CSS variable injected by KernelSU
 */
@Composable
expect fun statusBarTopPadding(): Dp

/**
 * Bottom padding for system navigation bars.
 * - Android: WindowInsets.navigationBars bottom padding
 * - wasmJs: reads --safe-area-inset-bottom CSS variable injected by KernelSU
 */
@Composable
expect fun navigationBarBottomPadding(): Dp
