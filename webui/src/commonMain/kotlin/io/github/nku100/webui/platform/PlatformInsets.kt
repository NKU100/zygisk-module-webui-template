package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Bottom padding for system navigation bars.
 * - Android: WindowInsets.navigationBars bottom padding
 * - wasmJs: 0.dp (no system navigation bars in browser)
 */
@Composable
expect fun navigationBarBottomPadding(): Dp
