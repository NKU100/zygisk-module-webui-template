package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// KernelSU converts physical insets to density-independent CSS pixels before injection.
// With width=device-width these values are already dp; dividing by DPR would shrink them.

@JsFun("() => parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--safe-area-inset-top')) || 0")
private external fun getSafeAreaInsetTopDp(): Float

@JsFun("() => parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--safe-area-inset-bottom')) || 0")
private external fun getSafeAreaInsetBottomDp(): Float

/**
 * Reads a safe-area-inset CSS variable (already converted to dp), polling until a
 * non-zero value appears or a timeout is reached. KernelSU injects the values
 * asynchronously after enableEdgeToEdge(true) is called.
 */
@Composable
private fun rememberSafeAreaInset(reader: () -> Float): Dp {
    val insetDp = produceState(reader().dp) {
        // Poll for up to 2 seconds to capture async inset injection
        var attempts = 0
        while (attempts < 20) {
            delay(100.milliseconds)
            val dp = reader()
            val newDp = dp.dp
            if (newDp != value) value = newDp
            if (dp > 0f) break
            attempts++
        }
    }
    return insetDp.value
}

@Composable
actual fun statusBarTopPadding(): Dp = rememberSafeAreaInset(::getSafeAreaInsetTopDp)

@Composable
actual fun navigationBarBottomPadding(): Dp = rememberSafeAreaInset(::getSafeAreaInsetBottomDp)
