package io.github.nku100.webui.ui.screen.logs

import io.github.nku100.webui.ui.component.SearchStatus

data class LogLine(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val raw: String,
)

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN }

data class LogsUiState(
    val lines: List<LogLine> = emptyList(),
    val visibleLines: List<LogLine> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchStatus: SearchStatus = SearchStatus(label = "Search logs…"),
    val selectedLevel: LogLevel? = null,
    val errorMessage: String? = null,
)

data class LogsActions(
    val onRefresh: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onSearchStatusChange: (SearchStatus) -> Unit = {},
    val onSelectLevel: (LogLevel?) -> Unit = {},
)

