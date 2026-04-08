package io.github.nku100.webui.data

import kotlinx.serialization.Serializable

/**
 * Module configuration stored as JSON.
 * Extensible: add new fields as needed for your module.
 */
@Serializable
data class ModuleConfig(
    /** Package names that this module should target. */
    val targetPackages: List<String> = emptyList(),
    /** Whether the module is enabled. */
    val enabled: Boolean = true,
    /** Per-package settings. Key is packageName. */
    val packageSettings: Map<String, PackageSettings> = emptyMap(),
    /** Theme mode: FOLLOW_SYSTEM, LIGHT, DARK */
    val themeMode: String = "FOLLOW_SYSTEM",
    /** Color style: DYNAMIC, DEFAULT, TEAL, ORANGE, PINK */
    val colorStyle: String = "DEFAULT",
    /** Enable blur effects */
    val enableBlur: Boolean = true,
    /** Enable floating bottom bar */
    val enableFloatingBottomBar: Boolean = true,
    /** Enable glass effect on floating bottom bar */
    val enableFloatingBottomBarBlur: Boolean = true,
)

/**
 * Per-package settings. Extend with module-specific fields.
 */
@Serializable
data class PackageSettings(
    /** Log level: DEBUG, INFO, WARN */
    val logLevel: String = "INFO",
    /** Custom log tag. Defaults to the app's short package name. */
    val logTag: String = "",
    /** Whether to dump the call stack trace in log output. */
    val dumpStackTrace: Boolean = false,
    /** Optional note/description for this package. */
    val note: String = "",
)
