package io.github.nku100.webui.platform

import androidx.compose.runtime.withFrameNanos
import kotlin.time.TimeSource

private val startMark = TimeSource.Monotonic.markNow()

actual suspend fun awaitNextFrame() {
    withFrameNanos { }
}

actual fun currentTimeMillis(): Long =
    (TimeSource.Monotonic.markNow() - startMark).inWholeMilliseconds
