package io.github.nku100.webui.ui.screen.logs

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.ui.component.SearchBox
import io.github.nku100.webui.ui.component.SearchPager
import io.github.nku100.webui.ui.component.SearchStatus
import io.github.nku100.webui.ui.component.StatusTag
import io.github.nku100.webui.ui.util.defaultHazeEffect
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun LogsPage(
    state: LogsUiState,
    actions: LogsActions,
    bottomPadding: Dp,
    enableBlur: Boolean = false,
) {
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

    val lazyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(state.visibleLines.size) {
        if (state.visibleLines.isNotEmpty()) {
            lazyListState.animateScrollToItem(state.visibleLines.size - 1)
        }
    }

    val showFilterPopup = remember { mutableStateOf(false) }
    val detailLine = remember { mutableStateOf<LogLine?>(null) }

    val searchStatus = state.searchStatus
    val levelOptions = listOf(null) + LogLevel.entries.toList()
    val levelLabels = listOf("All") + LogLevel.entries.map {
        it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
    }

    Scaffold(
        topBar = {
            searchStatus.TopAppBarAnim(
                hazeState = if (enableBlur) hazeState else null,
                hazeStyle = if (enableBlur) hazeStyle else null,
            ) {
                TopAppBar(
                    color = if (enableBlur) Color.Transparent else colorScheme.surface,
                    title = "Logs",
                    actions = {
                        // Level filter popup
                        SuperListPopup(
                            show = showFilterPopup.value,
                            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                            alignment = PopupPositionProvider.Align.TopEnd,
                            onDismissRequest = { showFilterPopup.value = false },
                            content = {
                                ListPopupColumn {
                                    levelLabels.forEachIndexed { index, label ->
                                        DropdownImpl(
                                            text = label,
                                            optionSize = levelLabels.size,
                                            isSelected = levelOptions[index] == state.selectedLevel,
                                            index = index,
                                            onSelectedIndexChange = {
                                                actions.onSelectLevel(levelOptions[it])
                                                showFilterPopup.value = false
                                            },
                                        )
                                    }
                                }
                            },
                        )
                        IconButton(
                            modifier = Modifier.padding(end = 4.dp),
                            onClick = { showFilterPopup.value = true },
                            holdDownState = showFilterPopup.value,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Filter,
                                tint = if (state.selectedLevel != null) colorScheme.primary else colorScheme.onSurface,
                                contentDescription = "Filter",
                            )
                        }
                        // Clear button
                        IconButton(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = actions.onClear,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                tint = colorScheme.onSurface,
                                contentDescription = "Clear logs",
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
                    // SearchPager default slot is unused for logs (the main list is in SearchBox content)
                },
                searchBarTopPadding = dynamicTopPadding,
            ) {
                val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().overScrollVertical(),
                ) {
                    item { Spacer(Modifier.height(6.dp)) }
                    when {
                        state.visibleLines.isEmpty() -> {
                            item {
                                LogMessageCard(
                                    message = if (searchStatus.searchText.isNotBlank()) "No matching logs." else "No logs yet."
                                )
                            }
                        }
                        else -> {
                            itemsIndexed(
                                items = state.visibleLines,
                                key = { index, _ -> index },
                            ) { _, line ->
                                LogLineItem(line = line, onClick = { detailLine.value = line })
                            }
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
            // Active level-filter chip row
            val chipTopPadding = innerPadding.calculateTopPadding() + boxHeight.value + 6.dp
            PullToRefresh(
                isRefreshing = state.isRefreshing,
                pullToRefreshState = pullToRefreshState,
                onRefresh = actions.onRefresh,
                refreshTexts = listOf("Pull to refresh", "Release to refresh", "Refreshing...", "Done"),
                contentPadding = PaddingValues(
                    top = chipTopPadding,
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
                        top = chipTopPadding,
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                    overscrollEffect = null,
                ) {
                    // Active filter chip
                    if (state.selectedLevel != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusTag(
                                    label = "≥ ${state.selectedLevel.name}",
                                    backgroundColor = levelColor(state.selectedLevel).copy(alpha = 0.15f),
                                    contentColor = levelColor(state.selectedLevel),
                                )
                            }
                        }
                    }

                    when {
                        state.isLoading -> {
                            item { LogMessageCard(message = "Loading…") }
                        }
                        state.errorMessage != null -> {
                            item { LogMessageCard(message = "Error: ${state.errorMessage}") }
                        }
                        state.visibleLines.isEmpty() -> {
                            item {
                                LogMessageCard(
                                    message = if (searchStatus.searchText.isNotBlank() || state.selectedLevel != null)
                                        "No matching logs."
                                    else
                                        "No logs yet. Tap refresh or trigger activity in a target app."
                                )
                            }
                        }
                        else -> {
                            item { SmallTitle(text = "${state.visibleLines.size} lines") }
                            itemsIndexed(
                                items = state.visibleLines,
                                key = { index, _ -> index },
                            ) { _, line ->
                                LogLineItem(line = line, onClick = { detailLine.value = line })
                            }
                        }
                    }

                    item { Spacer(Modifier.height(bottomPadding)) }
                }
            }
        }
    }

    // Detail dialog
    detailLine.value?.let { line ->
        SuperDialog(
            title = "${line.level.name}  ${line.tag}",
            show = true,
            onDismissRequest = { detailLine.value = null },
        ) {
            SelectionContainer {
                Text(
                    text = line.raw,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurface,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

// ── Log line item ────────────────────────────────────────────────────────────

@Composable
private fun LogLineItem(line: LogLine, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 4.dp),
        onClick = onClick,
        showIndication = true,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusTag(
                    label = line.level.name.take(1),
                    backgroundColor = levelColor(line.level).copy(alpha = 0.15f),
                    contentColor = levelColor(line.level),
                )
                if (line.tag.isNotBlank()) {
                    Text(
                        text = line.tag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
            if (line.message.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = line.message,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colorScheme.onSurfaceVariantSummary,
                    lineHeight = 16.sp,
                    maxLines = 5,
                )
            }
        }
    }
}

// ── Empty / error card ───────────────────────────────────────────────────────

@Composable
private fun LogMessageCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Article,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

// ── Level color ──────────────────────────────────────────────────────────────

@Composable
fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> colorScheme.onSurfaceVariantSummary
    LogLevel.DEBUG   -> Color(0xFF4DA6FF)
    LogLevel.INFO    -> Color(0xFF4CAF50)
    LogLevel.WARN    -> Color(0xFFFFA000)
    LogLevel.ERROR   -> Color(0xFFF44336)
    LogLevel.FATAL   -> Color(0xFFD50000)
    LogLevel.UNKNOWN -> colorScheme.onSurfaceVariantSummary
}
