package io.github.nku100.webui.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import io.github.nku100.webui.ui.modifier.inspectDragGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/**
 * wasmJs port of InteractiveHighlight (from KernelSU).
 *
 * Uses Skia RuntimeEffect (SkSL) instead of AGSL RuntimeShader.
 * The shader code is nearly identical — AGSL is based on SkSL.
 *
 * Differences from Android version:
 *   - android.graphics.RuntimeShader → org.jetbrains.skia.RuntimeEffect + RuntimeShaderBuilder
 *   - ShaderBrush(shader) → drawIntoCanvas with native Skia shader paint
 *   - fastCoerceIn → coerceIn
 */
actual class InteractiveHighlight actual constructor(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    actual val offset: Offset get() = positionAnimation.value - startPosition

    // SkSL shader — nearly identical to AGSL version
    private val runtimeEffect = RuntimeEffect.makeForShader("""
        uniform float2 size;
        uniform half4 color;
        uniform float radius;
        uniform float2 position;
        
        half4 main(float2 coord) {
            float dist = distance(coord, position);
            float intensity = smoothstep(radius, radius * 0.5, dist);
            return color * intensity;
        }
    """)

    actual val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(Color.White.copy(0.06f * progress), blendMode = BlendMode.Plus)

            val pos = position(size, positionAnimation.value)
            val clampedX = pos.x.coerceIn(0f, size.width)
            val clampedY = pos.y.coerceIn(0f, size.height)
            val colorAlpha = 0.12f * progress
            val r = size.minDimension * 1.2f

            val builder = RuntimeShaderBuilder(runtimeEffect)
            builder.uniform("size", size.width, size.height)
            builder.uniform("color", colorAlpha, colorAlpha, colorAlpha, colorAlpha) // premultiplied white
            builder.uniform("radius", r)
            builder.uniform("position", clampedX, clampedY)

            drawIntoCanvas { canvas ->
                val paint = org.jetbrains.skia.Paint().apply {
                    shader = builder.makeShader()
                    blendMode = org.jetbrains.skia.BlendMode.PLUS
                }
                canvas.nativeCanvas.drawRect(
                    org.jetbrains.skia.Rect.makeWH(size.width, size.height),
                    paint
                )
                paint.close()
            }
        }
        drawContent()
    }

    actual val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            }
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }
}
