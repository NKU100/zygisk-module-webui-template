package io.github.nku100.webui.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.nku100.webui.data.ConfigRepository
import io.github.nku100.webui.data.ModuleConfig
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.platform.PlatformBridge
import io.github.nku100.webui.ui.theme.AppTheme
import io.github.nku100.webui.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Cottage),
    APPS("Apps", Icons.Rounded.Security),
    LOGS("Logs", Icons.Rounded.Extension),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

@JsFun("() => typeof window !== 'undefined' && typeof window.ksu !== 'undefined' && window.ksu != null")
private external fun hasKsuApi(): Boolean

@Composable
actual fun MainScreen() {
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(ModuleConfig()) }
    var packages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val themeMode = remember(config.themeMode) {
        ThemeMode.entries.find { it.name == config.themeMode } ?: ThemeMode.FOLLOW_SYSTEM
    }

    LaunchedEffect(Unit) {
        if (hasKsuApi()) {
            try {
                config = ConfigRepository.load()
                packages = PlatformBridge.listPackages()
            } catch (_: Exception) {
                // KSU API error
            }
        } else {
            // Mock data for browser preview
            config = ModuleConfig(
                targetPackages = listOf("com.example.app", "com.android.chrome", "org.telegram.messenger"),
                enabled = true,
                enableFloatingBottomBar = true,
            )
            packages = listOf(
                PackageInfo(packageName = "com.android.chrome", label = "Chrome"),
                PackageInfo(packageName = "org.telegram.messenger", label = "Telegram"),
                PackageInfo(packageName = "com.example.app", label = "Example App"),
                PackageInfo(packageName = "com.whatsapp", label = "WhatsApp"),
                PackageInfo(packageName = "com.spotify.music", label = "Spotify"),
                PackageInfo(packageName = "com.instagram.android", label = "Instagram"),
                PackageInfo(packageName = "com.twitter.android", label = "X (Twitter)"),
            )
        }
        loading = false
    }

    fun saveConfig(newConfig: ModuleConfig) {
        config = newConfig
        if (hasKsuApi()) {
            scope.launch { ConfigRepository.save(newConfig) }
        }
    }

    AppTheme(themeMode = themeMode) {
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
                WasmPlaceholderPage(
                    tab = BottomTab.entries[page],
                    config = config,
                    packages = packages,
                    loading = loading,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    onConfigChange = ::saveConfig,
                    onNavigateToPage = { target ->
                        scope.launch { pagerState.animateScrollToPage(target) }
                    },
                )
            }
        }
    }
}
