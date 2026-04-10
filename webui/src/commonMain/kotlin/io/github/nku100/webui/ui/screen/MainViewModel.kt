package io.github.nku100.webui.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nku100.webui.ModuleInfo
import io.github.nku100.webui.data.ConfigRepository
import io.github.nku100.webui.data.ModuleConfig
import io.github.nku100.webui.data.PackageSettings
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.platform.PlatformBridge
import io.github.nku100.webui.platform.awaitNextFrame
import io.github.nku100.webui.platform.hasPlatformApi
import io.github.nku100.webui.ui.component.SearchStatus
import io.github.nku100.webui.ui.theme.ThemeMode
import io.github.nku100.webui.ui.screen.settings.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

data class MainUiState(
    val config: ModuleConfig = ModuleConfig(),
    val packages: List<PackageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val showSystemApps: Boolean = false,
    val appsSearchStatus: SearchStatus = SearchStatus(label = "Search apps..."),
    val searchResults: List<PackageInfo> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
    val updateChannelVisible: Boolean = false,
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        // Launch search query collector with debounce, mirroring KSU's launchSearchQueryCollector
        viewModelScope.launch {
            var debounceJob: Job? = null
            searchQuery.collect { text ->
                debounceJob?.cancel()
                debounceJob = launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    applySearchText(text)
                }
            }
        }

        // Auto-load data on init
        viewModelScope.launch {
            if (hasPlatformApi()) {
                fetchData()
            } else {
                loadMockData()
            }
        }
    }

    private suspend fun fetchData() {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val config = ConfigRepository.load()
            val rawPackages = PlatformBridge.listPackages()
            val targets = config.targetPackages.toSet()
            val packages = withContext(Dispatchers.Default) { sortPackages(rawPackages, targets) }
            val channel = readUpdateChannelFromProp()
            _uiState.update {
                it.copy(
                    config = config,
                    packages = packages,
                    isLoading = false,
                    hasLoaded = true,
                    themeMode = resolveThemeMode(config),
                    updateChannel = channel ?: UpdateChannel.STABLE,
                    updateChannelVisible = channel != null,
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, hasLoaded = true) }
        }
    }

    fun refresh(): Job = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true) }
        awaitNextFrame()
        try {
            val rawPackages = PlatformBridge.listPackages()
            val targets = _uiState.value.config.targetPackages.toSet()
            val packages = withContext(Dispatchers.Default) { sortPackages(rawPackages, targets) }
            awaitNextFrame()
            _uiState.update { it.copy(packages = packages, isRefreshing = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadMockData() {
        val targets = setOf("com.example.app", "com.android.chrome", "org.telegram.messenger")
        val rawPackages = listOf(
            PackageInfo(packageName = "com.android.chrome", label = "Chrome"),
            PackageInfo(packageName = "org.telegram.messenger", label = "Telegram"),
            PackageInfo(packageName = "com.example.app", label = "Example App"),
            PackageInfo(packageName = "com.whatsapp", label = "WhatsApp"),
            PackageInfo(packageName = "com.spotify.music", label = "Spotify"),
            PackageInfo(packageName = "com.instagram.android", label = "Instagram"),
            PackageInfo(packageName = "com.twitter.android", label = "X (Twitter)"),
            PackageInfo(packageName = "com.android.settings", label = "Settings", isSystemApp = true),
            PackageInfo(packageName = "com.android.systemui", label = "System UI", isSystemApp = true),
        )
        _uiState.update {
            it.copy(
                config = ModuleConfig(
                    targetPackages = targets.toList(),
                    enabled = true,
                    enableFloatingBottomBar = true,
                ),
                packages = sortPackages(rawPackages, targets),
                isLoading = false,
                hasLoaded = true,
            )
        }
    }

    private fun saveConfig(newConfig: ModuleConfig) {
        _uiState.update { it.copy(config = newConfig, themeMode = resolveThemeMode(newConfig)) }
        viewModelScope.launch { ConfigRepository.save(newConfig) }
    }

    fun setEnabled(enabled: Boolean) =
        saveConfig(_uiState.value.config.copy(enabled = enabled))

    fun setThemeMode(mode: ThemeMode) =
        saveConfig(_uiState.value.config.copy(themeMode = mode.name))

    fun setEnableBlur(enabled: Boolean) =
        saveConfig(_uiState.value.config.copy(enableBlur = enabled))

    fun setEnableFloatingBottomBar(enabled: Boolean) =
        saveConfig(_uiState.value.config.copy(enableFloatingBottomBar = enabled))

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) =
        saveConfig(_uiState.value.config.copy(enableFloatingBottomBarBlur = enabled))

    fun setUpdateChannel(channel: UpdateChannel) {
        _uiState.update { it.copy(updateChannel = channel) }
        viewModelScope.launch { writeUpdateChannelToProp(channel) }
    }

    /**
     * Read the current updateJson URL from module.prop and determine the channel.
     * Stable URLs contain "/releases/latest/download/";
     * Beta (CI) URLs contain "/releases/download/ci/".
     * Returns null if no updateJson is found or the URL doesn't match either pattern.
     */
    private suspend fun readUpdateChannelFromProp(): UpdateChannel? {
        return try {
            val content = PlatformBridge.readFile(ModuleInfo.MODULE_PROP_PATH)
            val url = content.lines()
                .firstOrNull { it.startsWith("updateJson=") }
                ?.removePrefix("updateJson=")
                ?.trim()
                ?: return null
            when {
                url.contains(STABLE_PATH) -> UpdateChannel.STABLE
                url.contains(BETA_PATH) -> UpdateChannel.BETA
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Rewrite the updateJson line in module.prop by swapping the release path segment.
     * Stable ↔ Beta is a simple text replacement of the path portion.
     */
    private suspend fun writeUpdateChannelToProp(channel: UpdateChannel) {
        try {
            val content = PlatformBridge.readFile(ModuleInfo.MODULE_PROP_PATH)
            if (content.isBlank()) return
            val (from, to) = when (channel) {
                UpdateChannel.STABLE -> BETA_PATH to STABLE_PATH
                UpdateChannel.BETA -> STABLE_PATH to BETA_PATH
            }
            val newContent = content.lines().joinToString("\n") { line ->
                if (line.startsWith("updateJson=")) line.replace(from, to) else line
            }
            PlatformBridge.writeFile(ModuleInfo.MODULE_PROP_PATH, newContent)
        } catch (_: Exception) { /* best-effort */ }
    }

    companion object {
        private const val STABLE_PATH = "/releases/latest/download/"
        private const val BETA_PATH = "/releases/download/ci/"
    }

    fun toggleTargetPackage(packageName: String, enabled: Boolean) {
        val config = _uiState.value.config
        val newTargets = if (enabled) config.targetPackages + packageName
                         else config.targetPackages - packageName
        val newConfig = config.copy(targetPackages = newTargets)
        // Don't re-sort here — mirrors KSU behavior where sort order updates on next refresh,
        // preventing the list from jumping while user is on the AppProfile page.
        _uiState.update { it.copy(config = newConfig, themeMode = resolveThemeMode(newConfig)) }
        viewModelScope.launch { ConfigRepository.save(newConfig) }
    }

    fun savePackageSettings(packageName: String, settings: PackageSettings) {
        val config = _uiState.value.config
        val newMap = config.packageSettings + (packageName to settings)
        saveConfig(config.copy(packageSettings = newMap))
    }

    fun getPackageSettings(packageName: String): PackageSettings =
        _uiState.value.config.packageSettings[packageName] ?: PackageSettings()

    fun toggleShowSystemApps(): Job {
        val newValue = !_uiState.value.showSystemApps
        _uiState.update { it.copy(showSystemApps = newValue) }
        // Re-apply search with new filter setting
        return viewModelScope.launch {
            applySearchText(_uiState.value.appsSearchStatus.searchText)
        }
    }

    fun updateSearchStatus(status: SearchStatus) {
        val previous = _uiState.value.appsSearchStatus
        _uiState.update { it.copy(appsSearchStatus = status) }
        if (previous.searchText != status.searchText) {
            searchQuery.value = status.searchText
        }
    }

    private suspend fun applySearchText(text: String) {
        // Set LOAD status while computing
        _uiState.update {
            it.copy(
                appsSearchStatus = it.appsSearchStatus.copy(
                    resultStatus = searchLoadingStatusFor(text)
                )
            )
        }

        if (text.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    appsSearchStatus = it.appsSearchStatus.copy(
                        resultStatus = SearchStatus.ResultStatus.DEFAULT
                    )
                )
            }
            return
        }

        val state = _uiState.value
        val targets = state.config.targetPackages.toSet()
        val sourceList = if (state.showSystemApps) state.packages
                         else state.packages.filter { !it.isSystemApp || it.packageName in targets }

        val result = withContext(Dispatchers.Default) {
            val filtered = sourceList.filter {
                it.label.contains(text, ignoreCase = true) ||
                    it.packageName.contains(text, ignoreCase = true)
            }
            sortPackages(filtered, targets)
        }

        _uiState.update {
            it.copy(
                searchResults = result,
                appsSearchStatus = it.appsSearchStatus.copy(
                    resultStatus = if (result.isEmpty()) SearchStatus.ResultStatus.EMPTY
                                   else SearchStatus.ResultStatus.SHOW
                )
            )
        }
    }

    private fun resolveThemeMode(config: ModuleConfig): ThemeMode =
        ThemeMode.entries.find { it.name == config.themeMode } ?: ThemeMode.FOLLOW_SYSTEM

    private fun searchLoadingStatusFor(text: String): SearchStatus.ResultStatus =
        if (text.isEmpty()) SearchStatus.ResultStatus.DEFAULT
        else SearchStatus.ResultStatus.LOAD

    /** Sort packages: targeted first (0), others last (1); within each group, sort by label. */
    private fun sortPackages(list: List<PackageInfo>, targetPackages: Set<String>): List<PackageInfo> =
        list.sortedWith(
            compareBy<PackageInfo> { if (it.packageName in targetPackages) 0 else 1 }
                .thenBy { it.label.lowercase() }
        )
}
