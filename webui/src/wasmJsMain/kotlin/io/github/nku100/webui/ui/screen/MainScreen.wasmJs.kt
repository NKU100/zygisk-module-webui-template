package io.github.nku100.webui.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.ui.component.WasmFloatingBottomBar
import io.github.nku100.webui.ui.component.WasmFloatingBottomBarItem
import io.github.nku100.webui.ui.theme.AppTheme
import io.github.nku100.webui.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
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
        val config = state.config
        val enableFloatingBottomBar = config.enableFloatingBottomBar
        val hazeState = remember { HazeState() }

        val pagerState = rememberPagerState(pageCount = { BottomTab.entries.size })

        var selectedPage by remember { mutableStateOf(0) }
        LaunchedEffect(pagerState.settledPage) {
            selectedPage = pagerState.settledPage
        }

        val items = BottomTab.entries.map { tab ->
            NavigationItem(label = tab.label, icon = tab.icon)
        }

        // Floating bar in bottomBar (same structure as Android)
        val bottomBar = @Composable {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (enableFloatingBottomBar) {
                    WasmFloatingBottomBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        selectedIndex = { selectedPage },
                        onSelected = { index ->
                            selectedPage = index
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        hazeState = hazeState,
                        tabsCount = items.size,
                        isDark = state.themeMode == ThemeMode.DARK,
                    ) {
                        items.forEachIndexed { index, item ->
                            WasmFloatingBottomBarItem(
                                onClick = {
                                    selectedPage = index
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selectedPage == index)
                                        MiuixTheme.colorScheme.primary
                                    else
                                        MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = if (selectedPage == index)
                                        MiuixTheme.colorScheme.primary
                                    else
                                        MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                } else {
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
        }

        Scaffold(bottomBar = bottomBar) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                state = pagerState,
                beyondViewportPageCount = 3,
                userScrollEnabled = true,
            ) { page ->
                PlaceholderPage(
                    tab = BottomTab.entries[page],
                    config = config,
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
