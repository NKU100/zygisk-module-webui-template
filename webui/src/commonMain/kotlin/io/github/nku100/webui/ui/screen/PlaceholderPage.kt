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
import io.github.nku100.webui.ui.navigation.LocalNavigator
import io.github.nku100.webui.ui.navigation.Route
import io.github.nku100.webui.ui.screen.apps.AppsActions
import io.github.nku100.webui.ui.screen.apps.AppsPage
import io.github.nku100.webui.ui.screen.apps.AppsUiState
import io.github.nku100.webui.ui.screen.home.HomeActions
import io.github.nku100.webui.ui.screen.home.HomePage
import io.github.nku100.webui.ui.screen.home.HomeUiState
import io.github.nku100.webui.ui.screen.settings.SettingsActions
import io.github.nku100.webui.ui.screen.settings.SettingsPage
import io.github.nku100.webui.ui.screen.settings.SettingsUiState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
                uiState = SettingsUiState.fromConfig(config),
                actions = SettingsActions(
                    onEnabledChange = { viewModel.setEnabled(it) },
                    onThemeModeChange = { viewModel.setThemeMode(it) },
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
