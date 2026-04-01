package io.github.nku100.webui.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Simplified floating bottom bar for wasmJs.
 * Uses ContinuousCapsule shape + haze blur (no backdrop dependency).
 */
@Composable
fun WasmFloatingBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    hazeState: HazeState,
    isDark: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer
    val style = HazeStyle(
        backgroundColor = surfaceColor.copy(alpha = 0.5f),
        tint = null,
    )

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(ContinuousCapsule)
                .hazeEffect(state = hazeState, style = style)
                .background(surfaceColor.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun RowScope.WasmFloatingBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scaleValue = animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
    )
    val bgAlphaValue = animateFloatAsState(
        targetValue = if (selected) 0.12f else 0f,
        animationSpec = spring(stiffness = 300f)
    )

    Column(
        modifier
            .clip(ContinuousCapsule)
            .background(MiuixTheme.colorScheme.primary.copy(alpha = bgAlphaValue.value))
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                scaleX = scaleValue.value
                scaleY = scaleValue.value
            },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
