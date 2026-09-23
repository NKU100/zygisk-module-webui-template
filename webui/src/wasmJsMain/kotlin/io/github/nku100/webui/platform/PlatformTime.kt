package io.github.nku100.webui.platform

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

actual suspend fun awaitNextFrame() {
    // withFrameNanos requires Compose frame loop context which is unavailable in ViewModel scope.
    // Use delay(16ms) to approximate one 60fps frame.
    delay(16.milliseconds)
}
