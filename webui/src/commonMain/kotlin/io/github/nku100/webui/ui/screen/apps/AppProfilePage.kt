package io.github.nku100.webui.ui.screen.apps
import org.jetbrains.compose.resources.stringResource
import zygisk_module_webui_template.webui.generated.resources.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.data.PackageSettings
import io.github.nku100.webui.platform.PackageInfo
import io.github.nku100.webui.ui.component.AppIconImage
import io.github.nku100.webui.ui.util.defaultHazeEffect
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

data class AppProfileUiState(
    val packageInfo: PackageInfo,
    val settings: PackageSettings,
    val isTargeted: Boolean,
)

data class AppProfileActions(
    val onBack: () -> Unit,
    val onSaveSettings: (PackageSettings) -> Unit,
    val onToggleTarget: (enabled: Boolean) -> Unit,
    val onLaunchApp: () -> Unit = {},
    val onForceStopApp: () -> Unit = {},
    val onRestartApp: () -> Unit = {},
)

@Composable
fun AppProfilePage(
    state: AppProfileUiState,
    actions: AppProfileActions,
    bottomPadding: Dp,
    enableBlur: Boolean = false,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = androidx.compose.runtime.remember { HazeState() }
    val hazeStyle = if (enableBlur) {
        HazeStyle(
            backgroundColor = colorScheme.surface,
            tint = HazeTint(colorScheme.surface.copy(0.8f))
        )
    } else {
        HazeStyle.Unspecified
    }

    val settings = state.settings

    // TextFields need local state for smooth typing; saved on value change with debounce effect
    var logTagLocal by rememberSaveable(settings.logTag) { mutableStateOf(settings.logTag) }
    var noteLocal by rememberSaveable(settings.note) { mutableStateOf(settings.note) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (enableBlur) Modifier.defaultHazeEffect(hazeState, hazeStyle) else Modifier,
                color = if (enableBlur) Color.Transparent else colorScheme.surface,
                title = state.packageInfo.label,
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = actions.onBack,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            tint = colorScheme.onSurface,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    val showPopup = androidx.compose.runtime.remember { mutableStateOf(false) }
                    top.yukonga.miuix.kmp.extra.SuperListPopup(
                        show = showPopup.value,
                        popupPositionProvider = top.yukonga.miuix.kmp.basic.ListPopupDefaults.ContextMenuPositionProvider,
                        alignment = top.yukonga.miuix.kmp.basic.PopupPositionProvider.Align.TopEnd,
                        onDismissRequest = { showPopup.value = false },
                        content = {
                            top.yukonga.miuix.kmp.basic.ListPopupColumn {
                                listOf(
                                    stringResource(Res.string.launch_app),
                                    stringResource(Res.string.force_stop),
                                    stringResource(Res.string.restart_app),
                                )
                                    .forEachIndexed { index, text ->
                                        top.yukonga.miuix.kmp.basic.DropdownImpl(
                                            text = text,
                                            optionSize = 3,
                                            isSelected = false,
                                            index = index,
                                            onSelectedIndexChange = { i ->
                                                when (i) {
                                                    0 -> actions.onLaunchApp()
                                                    1 -> actions.onForceStopApp()
                                                    2 -> actions.onRestartApp()
                                                }
                                                showPopup.value = false
                                            },
                                        )
                                    }
                            }
                        },
                    )
                    IconButton(
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { showPopup.value = true },
                        holdDownState = showPopup.value,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.MoreCircle,
                            tint = colorScheme.onSurface,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
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
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it },
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            // App header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // App icon
                    val pkg = state.packageInfo
                    AppIconImage(
                        iconModel = pkg.iconModel,
                        packageName = pkg.packageName,
                        contentDescription = pkg.label,
                        size = 64.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = pkg.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = pkg.packageName,
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // Enable section
            item {
                SmallTitle(text = stringResource(Res.string.section_module))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    SuperSwitch(
                        title = stringResource(Res.string.enable_for_this_app),
                        summary = stringResource(Res.string.enable_for_this_app_summary),
                        checked = state.isTargeted,
                        onCheckedChange = { actions.onToggleTarget(it) },
                    )
                }
            }

            // Log settings section
            item {
                SmallTitle(text = stringResource(Res.string.section_log_settings))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    val logLevelOptions = listOf("DEBUG", "INFO", "WARN")
                    SuperDropdown(
                        title = stringResource(Res.string.log_level),
                        summary = stringResource(Res.string.log_level_summary),
                        items = logLevelOptions,
                        selectedIndex = logLevelOptions.indexOf(settings.logLevel).coerceAtLeast(0),
                        onSelectedIndexChange = { idx ->
                            actions.onSaveSettings(settings.copy(logLevel = logLevelOptions[idx]))
                        },
                    )
                    SuperSwitch(
                        title = stringResource(Res.string.dump_stack_trace),
                        summary = stringResource(Res.string.dump_stack_trace_summary),
                        checked = settings.dumpStackTrace,
                        onCheckedChange = { actions.onSaveSettings(settings.copy(dumpStackTrace = it)) },
                    )
                }
            }

            // Log tag
            item {
                SmallTitle(text = stringResource(Res.string.section_log_tag))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.custom_tag),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.custom_tag_hint),
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    TextField(
                        value = logTagLocal,
                        onValueChange = {
                            logTagLocal = it
                            actions.onSaveSettings(settings.copy(logTag = it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = state.packageInfo.packageName.substringAfterLast('.'),
                        singleLine = true,
                    )
                }
            }

            // Note
            item {
                SmallTitle(text = stringResource(Res.string.section_note))
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    TextField(
                        value = noteLocal,
                        onValueChange = {
                            noteLocal = it
                            actions.onSaveSettings(settings.copy(note = it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.note_placeholder),
                    )
                }
            }

            item { Spacer(Modifier.height(bottomPadding)) }
        }
    }
}
