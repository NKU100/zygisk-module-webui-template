package io.github.nku100.webui.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nku100.webui.platform.BrowserHistorySync
import io.github.nku100.webui.platform.PlatformBridge
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.nku100.webui.ui.navigation.LocalNavigator
import io.github.nku100.webui.ui.navigation.Route
import io.github.nku100.webui.ui.navigation.rememberNavigator
import io.github.nku100.webui.ui.screen.MainScreen
import io.github.nku100.webui.ui.screen.MainPagerState
import io.github.nku100.webui.ui.screen.MainViewModel
import io.github.nku100.webui.ui.screen.apps.AppProfileActions
import io.github.nku100.webui.ui.screen.apps.AppProfilePage
import io.github.nku100.webui.ui.screen.apps.AppProfileUiState
import io.github.nku100.webui.ui.screen.settings.AboutPage
import io.github.nku100.webui.ui.theme.AppTheme

/**
 * Root App composable. Uses Navigation 3 NavDisplay for screen-level navigation.
 * AppTheme wraps the entire NavDisplay so all pages share the same theme.
 * Data loading and business logic are handled by MainViewModel.
 */
@Composable
fun App() {
    val viewModel = viewModel { MainViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val navigator = rememberNavigator(Route.Main)
    val mainPagerStateHolder = remember { mutableStateOf<MainPagerState?>(null) }

    AppTheme(themeMode = uiState.themeMode) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            // Must be at App level so it survives route navigation (MainScreen unmounts on push)
            BrowserHistorySync(navigator = navigator, mainPagerState = mainPagerStateHolder.value)

            NavDisplay(
                backStack = navigator.backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                ),
                onBack = { navigator.pop() },
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it })
                },
                popTransitionSpec = {
                    slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
                },
                predictivePopTransitionSpec = { _ ->
                    slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
                },
                entryProvider = entryProvider {
                    entry<Route.Main> {
                        MainScreen(viewModel, uiState, onPagerStateReady = { mainPagerStateHolder.value = it })
                    }
                    entry<Route.About> {
                        AboutPage(
                            bottomPadding = io.github.nku100.webui.platform.navigationBarBottomPadding(),
                            onBack = { navigator.pop() },
                            moduleAuthor = uiState.moduleAuthor,
                            enableBlur = uiState.config.enableBlur,
                        )
                    }
                    entry<Route.AppProfile> { key ->
                        val pkg = uiState.packages.find { it.packageName == key.packageName }
                            ?: return@entry
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        AppProfilePage(
                            state = AppProfileUiState(
                                packageInfo = pkg,
                                settings = viewModel.getPackageSettings(key.packageName),
                                isTargeted = uiState.config.targetPackages.contains(key.packageName),
                            ),
                            actions = AppProfileActions(
                                onBack = { navigator.pop() },
                                onSaveSettings = { settings ->
                                    viewModel.savePackageSettings(key.packageName, settings)
                                },
                                onToggleTarget = { enabled ->
                                    viewModel.toggleTargetPackage(key.packageName, enabled)
                                },
                                onLaunchApp = {
                                    scope.launch {
                                        PlatformBridge.exec(
                                            "cmd package resolve-activity --brief ${key.packageName} | tail -n 1 | xargs cmd activity start-activity -n"
                                        )
                                    }
                                },
                                onForceStopApp = {
                                    scope.launch {
                                        PlatformBridge.exec("am force-stop ${key.packageName}")
                                    }
                                },
                                onRestartApp = {
                                    scope.launch {
                                        PlatformBridge.exec("am force-stop ${key.packageName}")
                                        PlatformBridge.exec(
                                            "cmd package resolve-activity --brief ${key.packageName} | tail -n 1 | xargs cmd activity start-activity -n"
                                        )
                                    }
                                },
                            ),
                            bottomPadding = io.github.nku100.webui.platform.navigationBarBottomPadding(),
                            enableBlur = uiState.config.enableBlur,
                        )
                    }
                }
            )
        }
    }
}
