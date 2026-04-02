package io.github.nku100.webui.platform

import kotlinx.coroutines.android.awaitFrame

actual suspend fun awaitNextFrame() {
    awaitFrame()
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
