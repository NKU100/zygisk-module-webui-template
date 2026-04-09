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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.nku100.webui.platform.PlatformBackHandler
import org.jetbrains.compose.resources.stringResource
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
import io.github.nku100.webui.ui.util.defaultHazeEffect
import io.github.nku100.webui.platform.navigationBarBottomPadding
import io.github.nku100.webui.ui.component.FloatingBottomBar
import io.github.nku100.webui.ui.component.FloatingBottomBarItem
import io.github.nku100.webui.ui.theme.ThemeMode
import io.github.nku100.webui.ui.util.rememberContentReady
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Provides MainPagerState to the entire pager subtree, mirroring KSU's LocalMainPagerState. */
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}

@Composable
fun MainScreen(viewModel: MainViewModel, uiState: MainUiState) {
    val scope = rememberCoroutineScope()

    val config = uiState.config
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

    val pagerState = rememberPagerState(pageCount = { BottomTab.entries.size })
    val mainPagerState = rememberMainPagerState(pagerState, scope)

    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    val isBackHandlerEnabled by remember {
        derivedStateOf { mainPagerState.selectedPage != 0 }
    }
    PlatformBackHandler(enabled = isBackHandlerEnabled) {
        mainPagerState.animateToPage(0)
    }

    val items = BottomTab.entries.map { tab ->
        NavigationItem(label = stringResource(tab.labelRes), icon = tab.icon)
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
                    selectedIndex = { mainPagerState.selectedPage },
                    onSelected = { mainPagerState.animateToPage(it) },
                    backdrop = backdrop,
                    tabsCount = items.size,
                    isBlurEnabled = enableFloatingBottomBarBlur,
                    isDark = uiState.themeMode == ThemeMode.DARK,
                ) {
                    items.forEachIndexed { index, item ->
                        FloatingBottomBarItem(
                            onClick = { mainPagerState.animateToPage(index) },
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
                    modifier = if (config.enableBlur) Modifier.defaultHazeEffect(hazeState, hazeStyle) else Modifier,
                    color = if (config.enableBlur) Color.Transparent else MiuixTheme.colorScheme.surface,
                    content = {
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                icon = item.icon,
                                label = item.label,
                                selected = mainPagerState.selectedPage == index,
                                onClick = { mainPagerState.animateToPage(index) }
                            )
                        }
                    }
                )
            }
        }
    }

    CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
        val contentReady = rememberContentReady()

        Scaffold(bottomBar = bottomBar) { innerPadding ->
            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (config.enableBlur) Modifier.hazeSource(state = hazeState) else Modifier)
                    .then(if (enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                state = pagerState,
                beyondViewportPageCount = if (contentReady) 3 else 0,
                userScrollEnabled = true,
            ) { page ->
                val isCurrentPage = page == pagerState.settledPage
                if (isCurrentPage || contentReady) {
                    PlaceholderPage(
                        tab = BottomTab.entries[page],
                        uiState = uiState,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onNavigateToTab = { mainPagerState.animateToPage(it) },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}
