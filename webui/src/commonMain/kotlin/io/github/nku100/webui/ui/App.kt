package io.github.nku100.webui.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.nku100.webui.ui.navigation.LocalNavigator
import io.github.nku100.webui.ui.navigation.Route
import io.github.nku100.webui.ui.navigation.rememberNavigator
import io.github.nku100.webui.ui.screen.MainScreen
import io.github.nku100.webui.ui.screen.settings.AboutPage

/**
 * Root App composable. Uses Navigation 3 NavDisplay for screen-level navigation.
 * HorizontalPager tab switching is handled inside MainScreen.
 */
@Composable
fun App() {
    val navigator = rememberNavigator(Route.Main)

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
                entry<Route.Main> { MainScreen() }
                entry<Route.About> {
                    AboutPage(
                        bottomPadding = io.github.nku100.webui.platform.navigationBarBottomPadding(),
                        onBack = { navigator.pop() },
                    )
                }
            }
        )
    }
}
