package io.github.nku100.webui.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.nku100.webui.data.ConfigRepository
import io.github.nku100.webui.data.ModuleConfig
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.platform.PlatformBridge
import io.github.nku100.webui.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared state holder for MainScreen, used by both Android and wasmJs.
 */
class MainScreenState(private val scope: CoroutineScope) {
    var config by mutableStateOf(ModuleConfig())
    var packages by mutableStateOf<List<PackageInfo>>(emptyList())
    var loading by mutableStateOf(true)

    val themeMode: ThemeMode
        get() = ThemeMode.entries.find { it.name == config.themeMode } ?: ThemeMode.FOLLOW_SYSTEM

    fun saveConfig(newConfig: ModuleConfig) {
        config = newConfig
        scope.launch { ConfigRepository.save(newConfig) }
    }

    suspend fun loadFromPlatform() {
        config = ConfigRepository.load()
        packages = PlatformBridge.listPackages()
        loading = false
    }

    fun loadMockData() {
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
        loading = false
    }
}

/**
 * Root screen composable with multi-tab navigation.
 * Fully cross-platform — uses Backdrop + Haze + platform expect/actual for insets and API detection.
 */
