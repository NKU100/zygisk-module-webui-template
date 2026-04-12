package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import io.github.nku100.webui.ui.navigation.Navigator
import io.github.nku100.webui.ui.screen.MainPagerState

/** Android uses native BackHandler — no browser history sync needed. */
@Composable
actual fun BrowserHistorySync(navigator: Navigator, mainPagerState: MainPagerState?) {
    // no-op on Android
}
