package io.github.nku100.webui.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for Navigation 3.
 * KMP-compatible — no Parcelable dependency.
 */
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object About : Route

    @Serializable
    data class AppProfile(val packageName: String) : Route
}
