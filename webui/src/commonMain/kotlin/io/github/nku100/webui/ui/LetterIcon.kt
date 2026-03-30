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
 * A colored placeholder icon showing the first letter of the package name.
 * Used as fallback when no real app icon is available.
 */
@Composable
fun LetterIcon(packageName: String, modifier: Modifier = Modifier) {
    val initial = packageName.substringAfterLast('.').firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 18.sp,
        )
    }
}
