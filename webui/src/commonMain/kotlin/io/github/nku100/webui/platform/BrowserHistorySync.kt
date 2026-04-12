package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import io.github.nku100.webui.ui.navigation.Navigator
import io.github.nku100.webui.ui.screen.MainPagerState

/**
 * Synchronizes the app's navigation state with the browser's history stack (wasmJs only).
 *
 * On wasmJs, uses location.hash to create real navigation entries that WebView recognizes.
 * When the user swipes back, WebView calls goBack() which reverts the hash, and we handle
 * the Compose-side back navigation in the hashchange listener.
 *
 * On Android this is a no-op — native BackHandler handles back navigation.
 */
@Composable
expect fun BrowserHistorySync(navigator: Navigator, mainPagerState: MainPagerState?)
