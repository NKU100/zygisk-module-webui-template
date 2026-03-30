package io.github.nku100.webui.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * wasmJs: delegates to LetterIcon (no real app icon available in browser).
 */
@Composable
actual fun AppIcon(iconModel: Any?, packageName: String, modifier: Modifier) {
    LetterIcon(packageName = packageName, modifier = modifier)
}
