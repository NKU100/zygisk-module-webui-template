package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable

/**
 * Cross-platform BackHandler. On Android, intercepts the system back gesture/button.
 * On wasmJs, this is a no-op since there is no back button concept.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
