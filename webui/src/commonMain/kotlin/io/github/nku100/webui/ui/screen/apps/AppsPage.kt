package io.github.nku100.webui.ui.screen.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.ui.component.AppIconImage
import io.github.nku100.webui.ui.component.SearchBox
import io.github.nku100.webui.ui.component.SearchPager
import io.github.nku100.webui.ui.component.SearchStatus
import io.github.nku100.webui.ui.component.StatusTag
import io.github.nku100.webui.ui.util.defaultHazeEffect
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

data class AppsUiState(
    val packages: List<PackageInfo> = emptyList(),
    val targetPackages: Set<String> = emptySet(),
    val loading: Boolean = true,
    val hasLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val showSystemApps: Boolean = false,
    val searchStatus: SearchStatus = SearchStatus(label = "Search apps..."),
    val searchResults: List<PackageInfo> = emptyList(),
)

data class AppsActions(
    val onToggleTarget: (packageName: String, enabled: Boolean) -> Unit,
    val onRefresh: () -> Unit = {},
    val onToggleShowSystemApps: () -> Unit = {},
    val onSearchStatusChange: (SearchStatus) -> Unit = {},
    val onNavigateToProfile: (packageName: String) -> Unit = {},
)

@Composable
fun AppsPage(
    state: AppsUiState,
    actions: AppsActions,
    bottomPadding: Dp,
    enableBlur: Boolean = false,
) {
    val searchStatus = state.searchStatus
    val scrollBehavior = MiuixScrollBehavior()
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }

    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    // Hoist these outside SearchBox lambda so they survive config changes (e.g. toggling target)
    val lazyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val prevRefreshing = remember { booleanArrayOf(false) }
    if (prevRefreshing[0] && !state.isRefreshing) {
        lazyListState.requestScrollToItem(0)
    }
    prevRefreshing[0] = state.isRefreshing

    // displayPackages: list filtered by showSystemApps (for main list)
    // Targeted apps are always shown even if showSystemApps=false, mirroring KSU's filterAndSort
    val displayPackages by remember(state.packages, state.showSystemApps, state.targetPackages) {
        derivedStateOf {
            if (state.showSystemApps) state.packages
            else state.packages.filter { !it.isSystemApp || it.packageName in state.targetPackages }
        }
    }
    // searchResults come from ViewModel (already filtered + resultStatus managed)
    val searchResults = state.searchResults

    Scaffold(
        topBar = {
            searchStatus.TopAppBarAnim(
                hazeState = if (enableBlur) hazeState else null,
                hazeStyle = if (enableBlur) hazeStyle else null,
            ) {
                TopAppBar(
                    color = if (enableBlur) Color.Transparent else colorScheme.surface,
                    title = "Apps",
                    actions = {
                        val showTopPopup = remember { mutableStateOf(false) }
                        SuperListPopup(
                            show = showTopPopup.value,
                            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                            alignment = PopupPositionProvider.Align.TopEnd,
                            onDismissRequest = { showTopPopup.value = false },
                            content = {
                                ListPopupColumn {
                                    DropdownImpl(
                                        text = "Show system apps",
                                        isSelected = state.showSystemApps,
                                        optionSize = 1,
                                        onSelectedIndexChange = {
                                            actions.onToggleShowSystemApps()
                                            showTopPopup.value = false
                                        },
                                        index = 0,
                                    )
                                }
                            }
                        )
                        IconButton(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = { showTopPopup.value = true },
                            holdDownState = showTopPopup.value,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.MoreCircle,
                                tint = colorScheme.onSurface,
                                contentDescription = null,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = {
            searchStatus.SearchPager(
                onSearchStatusChange = actions.onSearchStatusChange,
                defaultResult = {
                    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().overScrollVertical(),
                    ) {
                        item { Spacer(Modifier.height(6.dp)) }
                        items(displayPackages, key = { it.packageName }) { pkg ->
                            val isTarget = state.targetPackages.contains(pkg.packageName)
                            AppItem(
                                packageInfo = pkg,
                                isTarget = isTarget,
                                onClick = { actions.onNavigateToProfile(pkg.packageName) },
                            )
                        }
                        item { Spacer(Modifier.height(maxOf(bottomPadding, imeBottomPadding))) }
                    }
                },
                searchBarTopPadding = dynamicTopPadding,
            ) {
                val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical(),
                ) {
                    item { Spacer(Modifier.height(6.dp)) }
                    items(searchResults, key = { it.packageName }) { pkg ->
                        val isTarget = state.targetPackages.contains(pkg.packageName)
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            AppItem(
                                packageInfo = pkg,
                                isTarget = isTarget,
                                onClick = { actions.onNavigateToProfile(pkg.packageName) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(maxOf(bottomPadding, imeBottomPadding))) }
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        searchStatus.SearchBox(
            onSearchStatusChange = actions.onSearchStatusChange,
            searchBarTopPadding = dynamicTopPadding,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
            hazeState = if (enableBlur) hazeState else null,
            hazeStyle = if (enableBlur) hazeStyle else null,
            enableBlur = enableBlur,
        ) { boxHeight ->
            if (displayPackages.isEmpty() && !state.hasLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                            bottom = bottomPadding,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    InfiniteProgressIndicator()
                }
            } else {
                PullToRefresh(
                    isRefreshing = state.isRefreshing,
                    pullToRefreshState = pullToRefreshState,
                    onRefresh = actions.onRefresh,
                    refreshTexts = listOf("Pull to refresh", "Release to refresh", "Refreshing...", "Done"),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + boxHeight.value + 6.dp,
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxHeight()
                            .scrollEndHaptic()
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .let { if (enableBlur) it.hazeSource(state = hazeState) else it },
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + boxHeight.value + 6.dp,
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        ),
                        overscrollEffect = null,
                    ) {
                        items(
                            items = displayPackages,
                            key = { it.packageName },
                        ) { pkg ->
                            val isTarget = state.targetPackages.contains(pkg.packageName)
                            AppItem(
                                packageInfo = pkg,
                                isTarget = isTarget,
                                onClick = { actions.onNavigateToProfile(pkg.packageName) },
                            )
                        }
                        item { Spacer(Modifier.height(bottomPadding)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    packageInfo: PackageInfo,
    isTarget: Boolean,
    onClick: () -> Unit,
) {
    val bg = colorScheme.secondaryContainer.copy(alpha = 0.8f)
    val fg = colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // App icon
            AppIconImage(
                    iconModel = packageInfo.iconModel,
                    packageName = packageInfo.packageName,
                    contentDescription = packageInfo.label,
                    modifier = Modifier.padding(end = 10.dp),
                    size = 48.dp,
                )

            // Label + package name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageInfo.label,
                    modifier = Modifier.basicMarquee(),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = packageInfo.packageName,
                    modifier = Modifier.basicMarquee(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            // Status tags
            if (isTarget) {
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatusTag(label = "TARGETED", backgroundColor = bg, contentColor = fg)
                }
            }

            // Arrow right
            val layoutDirection = LocalLayoutDirection.current
            Image(
                modifier = Modifier
                    .graphicsLayer {
                        if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                    }
                    .padding(start = 8.dp)
                    .size(width = 10.dp, height = 16.dp),
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorScheme.onSurfaceVariantActions),
            )
        }
    }
}
