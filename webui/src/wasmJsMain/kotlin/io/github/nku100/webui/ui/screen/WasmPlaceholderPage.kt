package io.github.nku100.webui.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nku100.webui.data.ModuleConfig
import io.github.nku100.webui.platform.PackageInfo
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun WasmPlaceholderPage(
    tab: BottomTab,
    config: ModuleConfig,
    packages: List<PackageInfo>,
    loading: Boolean,
    bottomPadding: Dp,
    onConfigChange: (ModuleConfig) -> Unit,
    onNavigateToPage: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tab.label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (tab) {
                BottomTab.HOME -> "Module Active • ${config.targetPackages.size} apps targeted"
                BottomTab.APPS -> "${packages.size} apps available • ${if (loading) "Loading..." else "Ready"}"
                BottomTab.LOGS -> "View runtime logs"
                BottomTab.SETTINGS -> "Theme & module settings"
            },
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
