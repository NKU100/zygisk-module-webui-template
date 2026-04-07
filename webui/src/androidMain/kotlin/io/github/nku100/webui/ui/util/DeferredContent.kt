package io.github.nku100.webui.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import io.github.nku100.webui.platform.awaitNextFrame

@Composable
actual fun rememberContentReady(): Boolean {
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
