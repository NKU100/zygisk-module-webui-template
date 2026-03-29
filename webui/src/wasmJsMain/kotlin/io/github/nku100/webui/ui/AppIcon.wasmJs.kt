package io.github.nku100.webui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * wasmJs: Display a colored placeholder with the first letter of the package name.
 * KernelSU-Next supports ksu://icon/{packageName} but standard KernelSU does not,
 * so we use a safe fallback. Replace with Image + ksu:// URI when targeting KernelSU-Next.
 */
@Composable
actual fun AppIcon(iconModel: Any?, packageName: String, modifier: Modifier) {
    val initial = packageName.substringAfterLast('.').firstOrNull()?.uppercase() ?: "?"
    val color = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 18.sp,
        )
    }
}
