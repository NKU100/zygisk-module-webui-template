package io.github.nku100.webui.ui.screen.settings
import org.jetbrains.compose.resources.stringResource
import io.github.nku100.webui.generated.resources.*

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
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Update
import io.github.nku100.webui.ui.util.rememberDefaultBlurBackdrop
import io.github.nku100.webui.ui.util.topBarDefaultWindowInsetsPadding
import io.github.nku100.webui.ui.util.topBarModifier
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
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
    val blurBackdrop = rememberDefaultBlurBackdrop(enableBlur)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.topBarModifier(blurBackdrop),
                color = if (enableBlur) Color.Transparent else colorScheme.surface,
                title = stringResource(Res.string.tab_settings),
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = topBarDefaultWindowInsetsPadding,
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
                .then(blurBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
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
                    SwitchPreference(
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

            // Theme Settings
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    ArrowPreference(
                        title = stringResource(Res.string.theme_settings),
                        summary = stringResource(Res.string.theme_settings_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.Palette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.theme_settings),
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = actions.onOpenThemeSettings,
                    )
                }
            }

            // Update Channel (only visible when module.prop updateJson matches known pattern)
            item {
                AnimatedVisibility(visible = uiState.updateChannelVisible) {
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        val channelItems = UpdateChannel.entries.map { channel ->
                            when (channel) {
                                UpdateChannel.STABLE -> stringResource(Res.string.update_channel_stable)
                                UpdateChannel.BETA -> stringResource(Res.string.update_channel_beta)
                            }
                        }
                        OverlayDropdownPreference(
                            items = channelItems,
                            title = stringResource(Res.string.update_channel),
                            summary = stringResource(Res.string.update_channel_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Update,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(Res.string.update_channel),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = UpdateChannel.entries.indexOf(uiState.updateChannel),
                            onSelectedIndexChange = { index ->
                                actions.onUpdateChannelChange(UpdateChannel.entries[index])
                            }
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
                    ArrowPreference(
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
