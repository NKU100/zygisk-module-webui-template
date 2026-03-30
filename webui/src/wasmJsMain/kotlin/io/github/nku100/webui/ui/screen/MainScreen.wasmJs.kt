package io.github.nku100.webui.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.nku100.webui.ui.theme.AppTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@JsFun("() => typeof window !== 'undefined' && typeof window.ksu !== 'undefined' && window.ksu != null")
private external fun hasKsuApi(): Boolean

@Composable
actual fun MainScreen() {
    val scope = rememberCoroutineScope()
    val state = remember { MainScreenState(scope) }

    LaunchedEffect(Unit) {
        if (hasKsuApi()) {
            try {
                state.loadFromPlatform()
            } catch (_: Exception) {
                state.loading = false
            }
        } else {
            state.loadMockData()
        }
    }

    AppTheme(themeMode = state.themeMode) {
        val pagerState = rememberPagerState(pageCount = { BottomTab.entries.size })

        var selectedPage by remember { mutableStateOf(0) }
        LaunchedEffect(pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }

        val items = BottomTab.entries.map { tab ->
            NavigationItem(label = tab.label, icon = tab.icon)
        }

        val bottomBar = @Composable {
            Box(modifier = Modifier.fillMaxWidth()) {
                NavigationBar(
                    color = MiuixTheme.colorScheme.surface,
                    content = {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                icon = item.icon,
                                label = item.label,
                                selected = selectedPage == index,
                                onClick = {
                                    selectedPage = index
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            )
                        }
                    }
                )
            }
        }

        Scaffold(bottomBar = bottomBar) { innerPadding ->
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                beyondViewportPageCount = 3,
                userScrollEnabled = true,
            ) { page ->
                PlaceholderPage(
                    tab = BottomTab.entries[page],
                    config = state.config,
                    packages = state.packages,
                    loading = state.loading,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    onConfigChange = { newConfig ->
                        if (hasKsuApi()) {
                            state.saveConfig(newConfig)
                        } else {
                            state.config = newConfig
                        }
                    },
                    onNavigateToPage = { target ->
                        scope.launch { pagerState.animateScrollToPage(target) }
                    },
                )
            }
        }
    }
}
