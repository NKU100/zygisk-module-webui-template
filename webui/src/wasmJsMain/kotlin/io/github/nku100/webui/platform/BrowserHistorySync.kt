package io.github.nku100.webui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.github.nku100.webui.ui.navigation.Navigator
import io.github.nku100.webui.ui.screen.MainPagerState

// Push N hash entries as guard layers, with syncing flag to suppress hashchange.
// setTimeout(0) defers the flag reset so queued hashchange events are still blocked.
@JsFun("""(n) => {
    window.__composeSyncing = true;
    for (var i = 1; i <= n; i++) location.hash = '#g' + i;
    setTimeout(function() { window.__composeSyncing = false; }, 0);
}""")
private external fun pushHashGuards(count: Int)

// Go back to the base entry (no hash), clearing all guard layers at once.
@JsFun("""(n) => {
    if (n > 0) {
        window.__composeSyncing = true;
        history.go(-n);
        setTimeout(function() { window.__composeSyncing = false; }, 0);
    }
}""")
private external fun clearGuards(count: Int)

// Get current guard depth from hash (e.g. "#g3" → 3, "" → 0)
@JsFun("() => { var h = location.hash; var m = h.match(/^#g(\\d+)$/); return m ? parseInt(m[1]) : 0; }")
private external fun currentGuardDepth(): Int

// Max guard layers to pre-push. Covers: up to 3 route pages + 1 tab layer.
private const val GUARD_DEPTH = 5

/**
 * Handles back navigation for KernelSU/KsuWebUIStandalone WebView.
 *
 * Strategy: pre-push multiple hash guard entries (#g1..#g5) when leaving the exit point.
 * Each goBack() consumes one layer. In the hashchange handler we perform the
 * Compose-side back action. When reaching the exit point, we clear all remaining
 * guards so the next back closes the Activity.
 *
 * This avoids the core issue: re-pushing hash in a hashchange handler is not
 * recognized by WebView's canGoBack() until the next event loop.
 */
@Composable
actual fun BrowserHistorySync(navigator: Navigator, mainPagerState: MainPagerState?) {
    val currentNavigator by rememberUpdatedState(navigator)
    val currentPagerState by rememberUpdatedState(mainPagerState)

    val needsGuard by remember(navigator, mainPagerState) {
        derivedStateOf {
            val hasRoutePages = navigator.backStackSize() > 1
            val isOnNonHomeTab = mainPagerState != null && mainPagerState.selectedPage != 0
            hasRoutePages || isOnNonHomeTab
        }
    }

    var hasGuards by remember { mutableStateOf(false) }

    // Push guard layers when navigating away from exit point
    LaunchedEffect(needsGuard) {
        if (needsGuard && !hasGuards) {
            pushHashGuards(GUARD_DEPTH)
            hasGuards = true
        }
    }

    // Listen for hashchange (goBack consumed one guard layer)
    DisposableEffect(Unit) {
        val listener: (JsAny?) -> Unit = listener@{ _ ->
            val depth = currentGuardDepth()
            val nav = currentNavigator
            val pager = currentPagerState

            if (depth < GUARD_DEPTH) {
                // A guard was consumed by goBack
                if (nav.backStackSize() > 1) {
                    // Pop a route page
                    nav.pop()
                } else if (pager != null && pager.selectedPage != 0) {
                    // Go back to home tab
                    pager.animateToPage(0)
                    // At exit point — clear remaining guards so next back exits
                    if (depth > 0) {
                        clearGuards(depth)
                        hasGuards = false
                    }
                } else if (depth > 0) {
                    // Already at exit point but still have guards — clear them
                    clearGuards(depth)
                    hasGuards = false
                }
            }
        }

        addHashChangeListener(listener)
        onDispose { removeHashChangeListener(listener) }
    }
}

// The hashchange handler checks window.__composeSyncing BEFORE calling
// the Kotlin callback. This ensures push/clear-triggered events are
// suppressed at the JS level, which is synchronous and reliable.
@JsFun("""
(callback) => {
    if (!window.__composeHashChangeHandler) {
        window.__composeHashChangeHandler = (event) => {
            if (window.__composeSyncing) return;
            if (window.__composeHashChangeCallback) {
                window.__composeHashChangeCallback(event);
            }
        };
        window.addEventListener('hashchange', window.__composeHashChangeHandler);
    }
    window.__composeHashChangeCallback = callback;
}
""")
private external fun addHashChangeListener(callback: (JsAny?) -> Unit)

@JsFun("""
(callback) => {
    window.__composeHashChangeCallback = null;
}
""")
private external fun removeHashChangeListener(callback: (JsAny?) -> Unit)
