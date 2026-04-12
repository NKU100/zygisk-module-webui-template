package io.github.nku100.webui.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Creates and remembers a [HazeState] and a default [HazeStyle] based on the current surface color.
 * Returns [HazeStyle.Unspecified] when [enableBlur] is false, so callers can skip blur logic uniformly.
 */
@Composable
fun rememberDefaultHazeState(enableBlur: Boolean): Pair<HazeState, HazeStyle> {
    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }
    return hazeState to hazeStyle
}

@OptIn(ExperimentalHazeApi::class)
fun Modifier.defaultHazeEffect(
    hazeState: HazeState,
    hazeStyle: HazeStyle,
): Modifier = this.hazeEffect(
    state = hazeState,
    style = hazeStyle
) {
    blurRadius = 20.dp
    inputScale = HazeInputScale.Fixed(0.35f)
    noiseFactor = 0f
    forceInvalidateOnPreDraw = false
}
