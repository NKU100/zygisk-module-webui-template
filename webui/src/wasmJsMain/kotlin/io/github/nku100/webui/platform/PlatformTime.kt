package io.github.nku100.webui.platform

import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private val startMark = TimeSource.Monotonic.markNow()

actual suspend fun awaitNextFrame() {
    // withFrameNanos requires Compose frame loop context which is unavailable in ViewModel scope.
    // Use delay(16) to approximate one 60fps frame.
    delay(16)
}

actual fun currentTimeMillis(): Long =
    (TimeSource.Monotonic.markNow() - startMark).inWholeMilliseconds
