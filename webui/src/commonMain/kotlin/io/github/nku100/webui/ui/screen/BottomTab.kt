package io.github.nku100.webui.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import zygisk_module_webui_template.webui.generated.resources.Res
import zygisk_module_webui_template.webui.generated.resources.tab_apps
import zygisk_module_webui_template.webui.generated.resources.tab_home
import zygisk_module_webui_template.webui.generated.resources.tab_logs
import zygisk_module_webui_template.webui.generated.resources.tab_settings

enum class BottomTab(val labelRes: StringResource, val icon: ImageVector) {
    HOME(Res.string.tab_home, Icons.Rounded.Cottage),
    APPS(Res.string.tab_apps, Icons.Rounded.Security),
    LOGS(Res.string.tab_logs, Icons.AutoMirrored.Rounded.TextSnippet),
    SETTINGS(Res.string.tab_settings, Icons.Rounded.Settings),
}
