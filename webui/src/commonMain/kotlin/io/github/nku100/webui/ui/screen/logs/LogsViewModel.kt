package io.github.nku100.webui.ui.screen.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.nku100.webui.ModuleInfo
import io.github.nku100.webui.platform.PlatformBridge
import io.github.nku100.webui.platform.awaitNextFrame
import io.github.nku100.webui.platform.hasPlatformApi
import io.github.nku100.webui.ui.component.SearchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!hasPlatformApi()) {
            _uiState.update { it.copy(lines = MOCK_LINES, visibleLines = MOCK_LINES) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            awaitNextFrame()
            fetchLogs()
        }
    }

    fun refresh() {
        if (!hasPlatformApi()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            awaitNextFrame()
            fetchLogs()
            awaitNextFrame()
        }
    }

    private suspend fun fetchLogs() {
        try {
            // Read the module log file written by the native side.
            // This avoids the ksu.exec callback size limit that causes logcat -d to fail silently
            // when the full logcat buffer is 20+ MB.
            val content = PlatformBridge.readFile(LOG_PATH)
            if (content.isBlank()) {
                _uiState.update {
                    it.copy(
                        lines = emptyList(),
                        visibleLines = emptyList(),
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
                return
            }
            val lines = withContext(Dispatchers.Default) {
                content.lines()
                    .filter { it.isNotBlank() }
                    .map(::parseLogLine)
            }
            _uiState.update { state ->
                val visible = applyFilters(lines, state.searchStatus.searchText, state.selectedLevel)
                state.copy(
                    lines = lines,
                    visibleLines = visible,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "Failed to read $LOG_PATH",
                )
            }
        }
    }

    fun clear() {
        if (!hasPlatformApi()) return
        viewModelScope.launch {
            // Truncate the log file
            PlatformBridge.writeFile(LOG_PATH, "")
            _uiState.update { it.copy(lines = emptyList(), visibleLines = emptyList(), errorMessage = null) }
        }
    }

    companion object {
        /** Must match LOG_PATH in zygisk_module.cpp */
        val LOG_PATH = ModuleInfo.CONFIG_PATH.replace("config.json", "module.log")

    }

    fun updateSearchStatus(status: SearchStatus) {
        _uiState.update { state ->
            val previous = state.searchStatus
            val newState = state.copy(searchStatus = status)
            if (previous.searchText != status.searchText) {
                val visible = applyFilters(state.lines, status.searchText, state.selectedLevel)
                newState.copy(visibleLines = visible)
            } else {
                newState
            }
        }
    }

    fun selectLevel(level: LogLevel?) {
        _uiState.update { state ->
            val visible = applyFilters(state.lines, state.searchStatus.searchText, level)
            state.copy(selectedLevel = level, visibleLines = visible)
        }
    }

    private fun applyFilters(
        lines: List<LogLine>,
        searchText: String,
        level: LogLevel?,
    ): List<LogLine> {
        var result = lines
        if (level != null) {
            val minOrdinal = level.ordinal
            result = result.filter { it.level.ordinal >= minOrdinal || it.level == LogLevel.UNKNOWN }
        }
        if (searchText.isNotBlank()) {
            result = result.filter {
                it.tag.contains(searchText, ignoreCase = true) ||
                    it.message.contains(searchText, ignoreCase = true)
            }
        }
        return result
    }

}

private val REGEX_THREADTIME = Regex(
    """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+(\S+)\s*:\s*(.*)$"""
)
private val REGEX_BRIEF = Regex("""^([VDIWEFS])/(\S+?)\(\s*(\d+)\):\s*(.*)$""")
private val REGEX_TAG = Regex("""^([VDIWEFS])/(\S+?):\s*(.*)$""")

internal fun parseLogLine(raw: String): LogLine {
    val threadtime = REGEX_THREADTIME.find(raw)
    if (threadtime != null) {
        val (timestamp, pid, tid, level, tag, message) = threadtime.destructured
        return LogLine(
            level = charToLevel(level[0]),
            tag = tag.trimEnd(':'),
            message = message,
            raw = raw,
            timestamp = timestamp,
            pid = pid,
            tid = tid,
        )
    }

    val brief = REGEX_BRIEF.find(raw)
    if (brief != null) {
        val (level, tag, pid, message) = brief.destructured
        return LogLine(
            level = charToLevel(level[0]),
            tag = tag,
            message = message,
            raw = raw,
            pid = pid,
        )
    }

    val tag = REGEX_TAG.find(raw)
    if (tag != null) {
        val (level, tagName, message) = tag.destructured
        return LogLine(level = charToLevel(level[0]), tag = tagName, message = message, raw = raw)
    }
    return LogLine(level = LogLevel.UNKNOWN, tag = "", message = raw, raw = raw)
}

private fun charToLevel(c: Char): LogLevel = when (c) {
    'V' -> LogLevel.VERBOSE
    'D' -> LogLevel.DEBUG
    'I' -> LogLevel.INFO
    'W' -> LogLevel.WARN
    'E' -> LogLevel.ERROR
    'F', 'S' -> LogLevel.FATAL
    else -> LogLevel.UNKNOWN
}

private val MOCK_LINES = listOf(
    parseLogLine("04-08 04:49:52.878  3285  3285 I ${ModuleInfo.LOG_TAG}: process=com.android.chrome logLevel=verbose dumpStackTrace=false"),
    parseLogLine("04-08 04:50:01.123  4000  4000 D ${ModuleInfo.LOG_TAG}: preAppSpecialize called for com.google.android.gms"),
    parseLogLine("04-08 04:50:02.456  4001  4001 W ${ModuleInfo.LOG_TAG}: config not found for com.example.unknown, skipping"),
    parseLogLine("04-08 04:50:03.789  4002  4002 E ${ModuleInfo.LOG_TAG}: Failed to open config.json: No such file or directory"),
    parseLogLine("04-08 04:50:05.000  4003  4003 V ${ModuleInfo.LOG_TAG}: onLoad complete"),
)
