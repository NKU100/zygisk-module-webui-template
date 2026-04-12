package io.github.nku100.webui.ui.util

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
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

/**
 * Whether TopAppBar should handle its own WindowInsets padding.
 * true on Android (system WindowInsets available), false on wasmJs (manually handled via [wasmStatusBarPadding]).
 */
val topBarDefaultWindowInsetsPadding: Boolean get() = isAndroidPlatform

/**
 * Modifier for TopAppBar: applies optional haze blur effect and wasmJs status bar padding.
 *
 * Replaces the repeated pattern across pages:
 * ```
 * modifier = (if (enableBlur) Modifier.defaultHazeEffect(hazeState, hazeStyle) else Modifier)
 *     .padding(top = statusBarPadding),
 * ```
 *
 * Used by pages where TopAppBar is directly in the topBar slot (HomePage, SettingsPage, AppProfilePage, AboutPage).
 */
@Composable
fun Modifier.topBarModifier(
    enableBlur: Boolean,
    hazeState: HazeState,
    hazeStyle: HazeStyle,
): Modifier {
    val statusBarPadding = wasmStatusBarPadding()
    return this
        .then(if (enableBlur) Modifier.defaultHazeEffect(hazeState, hazeStyle) else Modifier)
        .padding(top = statusBarPadding)
}

/**
 * Modifier that only applies wasmJs status bar top padding (no haze effect).
 *
 * Used by pages where TopAppBar is wrapped in [SearchStatus.TopAppBarAnim] (AppsPage, LogsPage),
 * since haze is handled by TopAppBarAnim itself.
 */
@Composable
fun Modifier.topBarInsetsPadding(): Modifier {
    val statusBarPadding = wasmStatusBarPadding()
    return this.padding(top = statusBarPadding)
}
