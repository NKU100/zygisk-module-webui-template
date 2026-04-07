package io.github.nku100.webui.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.nku100.webui.ui.navigation.LocalNavigator
import io.github.nku100.webui.ui.navigation.Route
import io.github.nku100.webui.ui.navigation.rememberNavigator
import io.github.nku100.webui.ui.screen.MainScreen
import io.github.nku100.webui.ui.screen.MainViewModel
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

    AppTheme(themeMode = uiState.themeMode) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
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
                    entry<Route.Main> { MainScreen(viewModel, uiState) }
                    entry<Route.About> {
                        AboutPage(
                            bottomPadding = io.github.nku100.webui.platform.navigationBarBottomPadding(),
                            onBack = { navigator.pop() },
                            enableBlur = uiState.config.enableBlur,
                        )
                    }
                }
            )
        }
    }
}
