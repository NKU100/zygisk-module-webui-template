package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // wasmJs has no system back button concept, no-op
}
