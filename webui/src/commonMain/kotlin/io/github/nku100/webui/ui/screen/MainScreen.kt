package io.github.nku100.webui.ui.screen

import androidx.compose.runtime.Composable

/**
 * Root screen composable with multi-tab navigation.
 * - Android: uses Miuix + FloatingBottomBar + Haze + Backdrop
 * - wasmJs: uses Material3 + standard NavigationBar
 */
@Composable
expect fun MainScreen()
