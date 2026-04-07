package io.github.nku100.webui.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import io.github.nku100.webui.platform.awaitNextFrame

/**
 * Ported from KernelSU: DeferredContent.kt
 *
 * Returns true only after the navigation transition animation has completed
 * and an additional buffer frame has passed.
 *
 * Timeline:
 * - During animation: returns false → HorizontalPager shows lightweight placeholder (smooth animation)
 * - Animation ends + 1 frame: returns true → heavy content composes
 *   (stutter is invisible because the page is already static)
 *
 * The value is sticky — once true it never reverts to false,
 * so content stays visible during exit transitions.
 */
@Composable
fun rememberContentReady(): Boolean {
    val scope = LocalNavAnimatedContentScope.current
    val transitionRunning = scope.transition.isRunning
    val ready = remember { mutableStateOf(false) }

    LaunchedEffect(transitionRunning) {
        if (!transitionRunning && !ready.value) {
            awaitNextFrame()
            ready.value = true
        }
    }

    return ready.value
}
