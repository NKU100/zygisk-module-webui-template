package io.github.nku100.webui.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nku100.webui.data.ModuleConfig
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.ui.navigation.LocalNavigator
import io.github.nku100.webui.ui.navigation.Route
import io.github.nku100.webui.ui.screen.home.HomeActions
import io.github.nku100.webui.ui.screen.home.HomePage
import io.github.nku100.webui.ui.screen.home.HomeUiState
import io.github.nku100.webui.ui.screen.settings.SettingsActions
import io.github.nku100.webui.ui.screen.settings.SettingsPage
import io.github.nku100.webui.ui.screen.settings.SettingsUiState
import io.github.nku100.webui.ui.theme.ThemeMode
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PlaceholderPage(
    tab: BottomTab,
    config: ModuleConfig,
    packages: List<PackageInfo>,
    loading: Boolean,
    bottomPadding: Dp,
    onConfigChange: (ModuleConfig) -> Unit,
    onNavigateToTab: (Int) -> Unit,
) {
    when (tab) {
        BottomTab.SETTINGS -> {
            val navigator = LocalNavigator.current
            val settingsState = SettingsUiState.fromConfig(config)
            val settingsActions = SettingsActions(
                onEnabledChange = { enabled ->
                    onConfigChange(config.copy(enabled = enabled))
                },
                onThemeModeChange = { mode ->
                    onConfigChange(config.copy(themeMode = mode.name))
                },
                onEnableBlurChange = { enabled ->
                    onConfigChange(config.copy(enableBlur = enabled))
                },
                onEnableFloatingBottomBarChange = { enabled ->
                    onConfigChange(config.copy(enableFloatingBottomBar = enabled))
                },
                onEnableFloatingBottomBarBlurChange = { enabled ->
                    onConfigChange(config.copy(enableFloatingBottomBarBlur = enabled))
                },
                onOpenAbout = { navigator.push(Route.About) },
            )
            SettingsPage(
                uiState = settingsState,
                actions = settingsActions,
                bottomPadding = bottomPadding,
                enableBlur = config.enableBlur,
            )
        }
        BottomTab.HOME -> {
            HomePage(
                state = HomeUiState(
                    moduleEnabled = config.enabled,
                    targetPackageCount = config.targetPackages.size,
                ),
                actions = HomeActions(
                    onStatusClick = { onNavigateToTab(BottomTab.SETTINGS.ordinal) },
                    onTargetAppsClick = { onNavigateToTab(BottomTab.APPS.ordinal) },
                ),
                bottomPadding = bottomPadding,
                enableBlur = config.enableBlur,
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = tab.label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (tab) {
                        BottomTab.APPS -> "${packages.size} apps available • ${if (loading) "Loading..." else "Ready"}"
                        BottomTab.LOGS -> "View runtime logs"
                        else -> ""
                    },
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
