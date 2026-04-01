package io.github.nku100.webui.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nku100.webui.ui.theme.ThemeMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun SettingsPage(
    uiState: SettingsUiState,
    actions: SettingsActions,
    bottomPadding: Dp,
    enableBlur: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = "Settings",
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            // Module Enabled
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SuperSwitch(
                        title = "Module Enabled",
                        summary = "Enable or disable the Zygisk module",
                        startAction = {
                            Icon(
                                Icons.Rounded.PowerSettingsNew,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "Module Enabled",
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = uiState.enabled,
                        onCheckedChange = actions.onEnabledChange
                    )
                }
            }

            // Theme Mode
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    val themeModeItems = ThemeMode.entries.map { it.name }
                    SuperDropdown(
                        title = "Theme Mode",
                        summary = "Choose light, dark, or follow system",
                        items = themeModeItems,
                        startAction = {
                            Icon(
                                Icons.Rounded.Palette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "Theme Mode",
                                tint = colorScheme.onBackground
                            )
                        },
                        selectedIndex = ThemeMode.entries.indexOf(uiState.themeMode),
                        onSelectedIndexChange = { index ->
                            actions.onThemeModeChange(ThemeMode.entries[index])
                        }
                    )
                }
            }

            // UI Effects
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    if (uiState.showAdvancedEffects) {
                        SuperSwitch(
                            title = "Blur Effects",
                            summary = "Enable blur effects (Android 13+)",
                            startAction = {
                                Icon(
                                    Icons.Rounded.BlurOn,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = "Blur Effects",
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableBlur,
                            onCheckedChange = actions.onEnableBlurChange
                        )
                    }
                    SuperSwitch(
                        title = "Floating Bottom Bar",
                        summary = "Use floating navigation bar style",
                        startAction = {
                            Icon(
                                Icons.Rounded.CallToAction,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "Floating Bottom Bar",
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = uiState.enableFloatingBottomBar,
                        onCheckedChange = actions.onEnableFloatingBottomBarChange
                    )
                    if (uiState.showAdvancedEffects) {
                        AnimatedVisibility(visible = uiState.enableFloatingBottomBar) {
                            SuperSwitch(
                                title = "Bottom Bar Glass Effect",
                                summary = "Enable glass blur on floating bottom bar",
                                startAction = {
                                    Icon(
                                        Icons.Rounded.WaterDrop,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = "Bottom Bar Glass Effect",
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.enableFloatingBottomBarBlur,
                                onCheckedChange = actions.onEnableFloatingBottomBarBlurChange
                            )
                        }
                    }
                }
            }

            // About
            item {
                Card(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SuperArrow(
                        title = "About",
                        summary = "Module info and version",
                        startAction = {
                            Icon(
                                Icons.Rounded.ContactPage,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "About",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = actions.onOpenAbout,
                    )
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}
