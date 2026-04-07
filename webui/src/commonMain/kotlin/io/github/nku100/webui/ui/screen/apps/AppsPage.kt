package io.github.nku100.webui.ui.screen.apps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.ui.util.defaultHazeEffect
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

data class AppsUiState(
    val packages: List<PackageInfo> = emptyList(),
    val targetPackages: Set<String> = emptySet(),
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
)

data class AppsActions(
    val onToggleTarget: (packageName: String, enabled: Boolean) -> Unit,
    val onRefresh: () -> Unit = {},
)

@Composable
fun AppsPage(
    state: AppsUiState,
    actions: AppsActions,
    bottomPadding: Dp,
    enableBlur: Boolean = false,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredPackages by remember(state.packages, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) state.packages
            else state.packages.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val targetCount = state.targetPackages.size

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (enableBlur) Modifier.defaultHazeEffect(hazeState, hazeStyle) else Modifier,
                color = if (enableBlur) Color.Transparent else colorScheme.surface,
                title = "Apps",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                InfiniteProgressIndicator()
            }
        } else {
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefresh(
                isRefreshing = state.isRefreshing,
                pullToRefreshState = pullToRefreshState,
                onRefresh = actions.onRefresh,
                refreshTexts = listOf("Pull to refresh", "Release to refresh", "Refreshing...", "Done"),
                contentPadding = innerPadding,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .let { if (enableBlur) it.hazeSource(state = hazeState) else it }
                        .padding(horizontal = 12.dp),
                    contentPadding = innerPadding,
                    overscrollEffect = null,
                ) {
                    // Search bar
                    item {
                        InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            label = "Search apps...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                        )
                    }

                    // Summary card
                    item {
                        Card(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = "$targetCount targeted",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${filteredPackages.size} of ${state.packages.size} apps",
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }

                    // App list
                    items(
                        items = filteredPackages,
                        key = { it.packageName },
                    ) { pkg ->
                        val isTarget = state.targetPackages.contains(pkg.packageName)
                        AppItem(
                            packageInfo = pkg,
                            isTarget = isTarget,
                            onToggle = { enabled ->
                                actions.onToggleTarget(pkg.packageName, enabled)
                            },
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(Modifier.height(bottomPadding + 12.dp))
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
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 10.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val initial = packageInfo.label.firstOrNull()?.uppercase() ?: "?"
                val bgColor = remember(packageInfo.packageName) {
                    val hash = packageInfo.packageName.hashCode()
                    Color(
                        red = ((hash and 0xFF0000) shr 16) / 255f * 0.6f + 0.2f,
                        green = ((hash and 0x00FF00) shr 8) / 255f * 0.6f + 0.2f,
                        blue = (hash and 0x0000FF) / 255f * 0.6f + 0.2f,
                        alpha = 1f,
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = bgColor)
                }
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = packageInfo.label,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = packageInfo.packageName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }

            Switch(
                modifier = Modifier.padding(end = 16.dp),
                checked = isTarget,
                onCheckedChange = onToggle,
            )
        }
    }
}
