package io.github.nku100.webui.platform

/**
 * Platform-specific time and frame utilities for animations.
 */

/**
 * Await the next frame. Used in release() animation timing.
 * - Android: kotlinx.coroutines.android.awaitFrame()
 * - wasmJs: withFrameNanos {}
 */
expect suspend fun awaitNextFrame()

/**
 * Current time in milliseconds for velocity tracking.
 * - Android: System.currentTimeMillis()
 * - wasmJs: TimeSource.Monotonic relative time
 */
expect fun currentTimeMillis(): Long
