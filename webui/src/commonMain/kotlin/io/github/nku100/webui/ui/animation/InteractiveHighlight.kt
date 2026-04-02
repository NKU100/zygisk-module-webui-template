package io.github.nku100.webui.ui.animation

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope

expect class InteractiveHighlight(
    animationScope: CoroutineScope,
    position: (size: Size, offset: Offset) -> Offset
) {
    val offset: Offset
    val modifier: Modifier
    val gestureModifier: Modifier
}
