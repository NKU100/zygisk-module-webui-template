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

class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    /**
     * Parse a single logcat line.
     *
     * Logcat formats handled:
     *   threadtime / time:  MM-DD HH:MM:SS.mmm  PID  TID  LEVEL tag  : message
     *   brief:              LEVEL/tag(PID): message
     *   tag:                LEVEL/tag: message
     *   raw / unknown:      treat entire line as message
     */
    private fun parseLine(raw: String): LogLine {
        // threadtime / time format: "04-08 12:34:56.789  1234  5678 I ZygiskWebUI: msg"
        val threadtime = Regex(
            """^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+\s+\d+\s+\d+\s+([VDIWEFS])\s+(\S+)\s*:\s*(.*)$"""
        ).find(raw)
        if (threadtime != null) {
            val (lvlChar, tag, msg) = threadtime.destructured
            return LogLine(level = charToLevel(lvlChar[0]), tag = tag.trimEnd(':'), message = msg, raw = raw)
        }
        // brief format: "I/ZygiskWebUI(1234): msg"
        val brief = Regex("""^([VDIWEFS])/(\S+?)\(\s*\d+\):\s*(.*)$""").find(raw)
        if (brief != null) {
            val (lvlChar, tag, msg) = brief.destructured
            return LogLine(level = charToLevel(lvlChar[0]), tag = tag, message = msg, raw = raw)
        }
        // tag format: "I/ZygiskWebUI: msg"
        val tag = Regex("""^([VDIWEFS])/(\S+?):\s*(.*)$""").find(raw)
        if (tag != null) {
            val (lvlChar, tagName, msg) = tag.destructured
            return LogLine(level = charToLevel(lvlChar[0]), tag = tagName, message = msg, raw = raw)
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
                    .map { parseLine(it) }
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
        /** Must match LOG_PATH in example.cpp */
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

    private val MOCK_LINES = listOf(
        parseLine("04-08 04:49:52.878  3285  3285 I SampleHook: [ZygiskWebUI] process=com.android.chrome logLevel=verbose dumpStackTrace=false"),
        parseLine("04-08 04:50:01.123  4000  4000 D SampleHook: [ZygiskWebUI] preAppSpecialize called for com.google.android.gms"),
        parseLine("04-08 04:50:02.456  4001  4001 W SampleHook: [ZygiskWebUI] config not found for com.example.unknown, skipping"),
        parseLine("04-08 04:50:03.789  4002  4002 E SampleHook: [ZygiskWebUI] Failed to open config.json: No such file or directory"),
        parseLine("04-08 04:50:05.000  4003  4003 V SampleHook: [ZygiskWebUI] onLoad complete"),
    )
}
