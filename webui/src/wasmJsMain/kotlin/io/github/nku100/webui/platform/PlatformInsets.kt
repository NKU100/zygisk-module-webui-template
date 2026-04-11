package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// JS interop: read CSS variables injected by KernelSU's evaluateJavascript(currentInsets.js).
// When enableEdgeToEdge(true) is called, KernelSU removes WebView margins and injects
// --safe-area-inset-* CSS variables on document.documentElement.style.
//
// IMPORTANT: The values are in Android physical pixels (from WindowInsetsCompat.getInsets()),
// but in a WebView with `width=device-width`, 1 CSS px ≈ 1 dp. We must divide by
// devicePixelRatio to convert from physical pixels to dp.

@JsFun("() => (parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--safe-area-inset-top')) || 0) / window.devicePixelRatio")
private external fun getSafeAreaInsetTopDp(): Float

@JsFun("() => (parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--safe-area-inset-bottom')) || 0) / window.devicePixelRatio")
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
