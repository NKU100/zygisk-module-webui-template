package io.github.nku100.webui.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import io.github.nku100.webui.ui.navigation.LocalNavigator
import io.github.nku100.webui.ui.navigation.Route
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nku100.webui.ui.screen.apps.AppsActions
import io.github.nku100.webui.ui.screen.apps.AppsPage
import io.github.nku100.webui.ui.screen.apps.AppsUiState
import io.github.nku100.webui.ui.screen.home.HomeActions
import io.github.nku100.webui.ui.screen.home.HomePage
import io.github.nku100.webui.ui.screen.home.HomeUiState
import io.github.nku100.webui.ui.screen.logs.LogsActions
import io.github.nku100.webui.ui.screen.logs.LogsPage
import io.github.nku100.webui.ui.screen.logs.LogsViewModel
import io.github.nku100.webui.ui.screen.settings.SettingsActions
import io.github.nku100.webui.ui.screen.settings.SettingsPage
import io.github.nku100.webui.ui.screen.settings.SettingsUiState

@Composable
fun PlaceholderPage(
    tab: BottomTab,
    uiState: MainUiState,
    bottomPadding: Dp,
    onNavigateToTab: (Int) -> Unit,
    viewModel: MainViewModel,
) {
    val config = uiState.config

    when (tab) {
        BottomTab.SETTINGS -> {
            val navigator = LocalNavigator.current
            SettingsPage(
                uiState = SettingsUiState.fromConfig(config, uiState.updateChannel, uiState.updateChannelVisible),
                actions = SettingsActions(
                    onEnabledChange = { viewModel.setEnabled(it) },
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onUpdateChannelChange = { viewModel.setUpdateChannel(it) },
                    onEnableBlurChange = { viewModel.setEnableBlur(it) },
                    onEnableFloatingBottomBarChange = { viewModel.setEnableFloatingBottomBar(it) },
                    onEnableFloatingBottomBarBlurChange = { viewModel.setEnableFloatingBottomBarBlur(it) },
                    onOpenAbout = { navigator.push(Route.About) },
                ),
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
        BottomTab.APPS -> {
            val navigator = LocalNavigator.current
            AppsPage(
                state = AppsUiState(
                    packages = uiState.packages,
                    targetPackages = config.targetPackages.toSet(),
                    loading = uiState.isLoading,
                    hasLoaded = uiState.hasLoaded,
                    isRefreshing = uiState.isRefreshing,
                    showSystemApps = uiState.showSystemApps,
                    searchStatus = uiState.appsSearchStatus,
                    searchResults = uiState.searchResults,
                ),
                actions = AppsActions(
                    onToggleTarget = { packageName, enabled ->
                        viewModel.toggleTargetPackage(packageName, enabled)
                    },
                    onRefresh = { viewModel.refresh() },
                    onToggleShowSystemApps = { viewModel.toggleShowSystemApps() },
                    onSearchStatusChange = { viewModel.updateSearchStatus(it) },
                    onNavigateToProfile = { packageName ->
                        navigator.push(Route.AppProfile(packageName))
                    },
                ),
                bottomPadding = bottomPadding,
                enableBlur = config.enableBlur,
            )
        }
        BottomTab.LOGS -> {
            val logsViewModel = viewModel { LogsViewModel() }
            val logsState by logsViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { logsViewModel.load() }
            LogsPage(
                state = logsState,
                actions = LogsActions(
                    onRefresh = { logsViewModel.refresh() },
                    onClear = { logsViewModel.clear() },
                    onSearchStatusChange = { logsViewModel.updateSearchStatus(it) },
                    onSelectLevel = { logsViewModel.selectLevel(it) },
                ),
                bottomPadding = bottomPadding,
                enableBlur = config.enableBlur,
            )
        }
    }
}
