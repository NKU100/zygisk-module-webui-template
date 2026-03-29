package io.github.nku100.webui.ui

import androidx.compose.runtime.Composable
import io.github.nku100.webui.ui.screen.MainScreen

/**
 * Root App composable. Delegates to MainScreen with multi-tab navigation.
 */
@Composable
fun App() {
    MainScreen()
}
