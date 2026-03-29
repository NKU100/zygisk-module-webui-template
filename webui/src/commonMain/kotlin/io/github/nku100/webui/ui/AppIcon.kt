package io.github.nku100.webui.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific app icon composable.
 * - wasmJs: renders an <img> via ksu://icon/ URI or a default icon
 * - Android: renders a Bitmap from PackageManager
 */
@Composable
expect fun AppIcon(iconModel: Any?, packageName: String, modifier: Modifier = Modifier)
