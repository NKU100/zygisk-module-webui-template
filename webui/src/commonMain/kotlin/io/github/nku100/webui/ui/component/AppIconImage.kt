package io.github.nku100.webui.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cross-platform app icon image.
 * - Android: renders from ApplicationInfo (iconModel) via AppIconCache
 * - wasmJs: loads from ksu://icon/<packageName> URL
 *
 * Falls back to [LetterIcon] when icon is not available.
 */
@Composable
expect fun AppIconImage(
    iconModel: Any?,
    packageName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
)
