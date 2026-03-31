package io.github.nku100.webui.ui.screen.settings

import io.github.nku100.webui.data.ModuleConfig
import io.github.nku100.webui.ui.theme.ThemeMode

data class SettingsUiState(
    val enabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = true,
    val enableFloatingBottomBarBlur: Boolean = true,
    /** Whether to show advanced UI effects (Blur/FloatingBar/Glass). False on wasmJs. */
    val showAdvancedEffects: Boolean = true,
) {
    companion object {
        fun fromConfig(config: ModuleConfig, isAndroid: Boolean = true): SettingsUiState {
            return SettingsUiState(
                enabled = config.enabled,
                themeMode = ThemeMode.entries.find { it.name == config.themeMode }
                    ?: ThemeMode.FOLLOW_SYSTEM,
                enableBlur = config.enableBlur,
                enableFloatingBottomBar = config.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = config.enableFloatingBottomBarBlur,
                showAdvancedEffects = isAndroid,
            )
        }
    }
}

data class SettingsActions(
    val onEnabledChange: (Boolean) -> Unit,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onEnableBlurChange: (Boolean) -> Unit,
    val onEnableFloatingBottomBarChange: (Boolean) -> Unit,
    val onEnableFloatingBottomBarBlurChange: (Boolean) -> Unit,
    val onOpenAbout: () -> Unit = {},
)
