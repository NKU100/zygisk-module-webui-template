package io.github.nku100.webui.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.platform.hasPlatformApi
import io.github.nku100.webui.platform.navigationBarBottomPadding
import io.github.nku100.webui.ui.component.FloatingBottomBar
import io.github.nku100.webui.ui.component.FloatingBottomBarItem
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

@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val state = remember { MainScreenState(scope) }

    LaunchedEffect(Unit) {
        if (hasPlatformApi()) {
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
        val enableFloatingBottomBarBlur = config.enableFloatingBottomBarBlur && enableFloatingBottomBar

        val surfaceColor = MiuixTheme.colorScheme.surface
        val hazeState = remember { HazeState() }
        val hazeStyle = if (config.enableBlur) {
            HazeStyle(
                backgroundColor = surfaceColor,
                tint = HazeTint(surfaceColor.copy(0.8f))
            )
        } else {
            HazeStyle.Unspecified
        }

        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }

        var selectedPage by rememberSaveable { mutableStateOf(0) }

        val pagerState = rememberPagerState(
            initialPage = selectedPage,
            pageCount = { BottomTab.entries.size },
        )

        LaunchedEffect(pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }

        val items = BottomTab.entries.map { tab ->
            NavigationItem(label = tab.label, icon = tab.icon)
        }

        val bottomBar = @Composable {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (enableFloatingBottomBar) {
                    FloatingBottomBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                            .padding(
                                bottom = 12.dp + navigationBarBottomPadding()
                            ),
                        selectedIndex = { selectedPage },
                        onSelected = { index ->
                            selectedPage = index
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        backdrop = backdrop,
                        tabsCount = items.size,
                        isBlurEnabled = enableFloatingBottomBarBlur,
                        isDark = state.themeMode == ThemeMode.DARK,
                    ) {
                        items.forEachIndexed { index, item ->
                            FloatingBottomBarItem(
                                onClick = {
                                    selectedPage = index
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                } else {
                    NavigationBar(
                        color = if (config.enableBlur) Color.Transparent else MiuixTheme.colorScheme.surface,
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
                    .then(if (config.enableBlur) Modifier.hazeSource(state = hazeState) else Modifier)
                    .then(if (enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
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
                        if (hasPlatformApi()) {
                            state.saveConfig(newConfig)
                        } else {
                            state.config = newConfig
                        }
                    },
                    onNavigateToTab = { index ->
                        selectedPage = index
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            }
        }
    }
}
