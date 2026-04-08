package io.github.nku100.webui.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Cottage),
    APPS("Apps", Icons.Rounded.Security),
    LOGS("Logs", Icons.AutoMirrored.Rounded.TextSnippet),
    SETTINGS("Settings", Icons.Rounded.Settings),
}
