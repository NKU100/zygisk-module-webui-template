package io.github.nku100.webui.ui.screen.settings
import org.jetbrains.compose.resources.stringResource
import zygisk_module_webui_template.webui.generated.resources.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.ui.theme.ThemeMode
import io.github.nku100.webui.ui.util.defaultHazeEffect
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
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsPage(
    uiState: SettingsUiState,
    actions: SettingsActions,
    bottomPadding: Dp,
    enableBlur: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (enableBlur) Modifier.defaultHazeEffect(hazeState, hazeStyle) else Modifier,
                color = if (enableBlur) Color.Transparent else colorScheme.surface,
                title = stringResource(Res.string.tab_settings),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it }
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
                        title = stringResource(Res.string.module_enabled),
                        summary = stringResource(Res.string.module_enabled_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.PowerSettingsNew,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.module_enabled),
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
                    val themeModeItems = ThemeMode.entries.map { mode ->
                        when (mode) {
                            ThemeMode.FOLLOW_SYSTEM -> stringResource(Res.string.theme_follow_system)
                            ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
                            ThemeMode.DARK -> stringResource(Res.string.theme_dark)
                        }
                    }
                    SuperDropdown(
                        title = stringResource(Res.string.theme_mode),
                        summary = stringResource(Res.string.theme_mode_summary),
                        items = themeModeItems,
                        startAction = {
                            Icon(
                                Icons.Rounded.Palette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.theme_mode),
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
                    SuperSwitch(
                        title = stringResource(Res.string.blur_effects),
                        summary = stringResource(Res.string.blur_effects_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.BlurOn,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.blur_effects),
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = uiState.enableBlur,
                        onCheckedChange = actions.onEnableBlurChange
                    )
                    SuperSwitch(
                        title = stringResource(Res.string.floating_bottom_bar),
                        summary = stringResource(Res.string.floating_bottom_bar_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.CallToAction,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.floating_bottom_bar),
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = uiState.enableFloatingBottomBar,
                        onCheckedChange = actions.onEnableFloatingBottomBarChange
                    )
                    AnimatedVisibility(visible = uiState.enableFloatingBottomBar) {
                        SuperSwitch(
                            title = stringResource(Res.string.bottom_bar_glass_effect),
                            summary = stringResource(Res.string.bottom_bar_glass_effect_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.WaterDrop,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(Res.string.bottom_bar_glass_effect),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.enableFloatingBottomBarBlur,
                            onCheckedChange = actions.onEnableFloatingBottomBarBlurChange
                        )
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
                        title = stringResource(Res.string.about),
                        summary = stringResource(Res.string.about_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.ContactPage,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.about),
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
