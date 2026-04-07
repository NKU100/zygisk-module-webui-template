package io.github.nku100.webui.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.nku100.webui.ModuleInfo
import io.github.nku100.webui.platform.openUrl
import io.github.nku100.webui.ui.theme.isSystemDarkTheme
import io.github.nku100.webui.ui.util.defaultHazeEffect
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

data class HomeUiState(
    val moduleEnabled: Boolean = true,
    val targetPackageCount: Int = 0,
    val moduleId: String = ModuleInfo.MODULE_ID,
    val moduleName: String = ModuleInfo.MODULE_NAME,
    val moduleVersion: String = ModuleInfo.MODULE_VERSION,
    val configPath: String = ModuleInfo.CONFIG_PATH,
)

data class HomeActions(
    val onStatusClick: () -> Unit,
    val onTargetAppsClick: () -> Unit,
)

@Composable
fun HomePage(
    state: HomeUiState,
    actions: HomeActions,
    bottomPadding: Dp,
    enableBlur: Boolean = false,
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
                title = ModuleInfo.MODULE_NAME,
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
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it }
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusCard(state = state, actions = actions)
                    InfoCard(state = state)
                    SourceCodeCard(onOpenUrl = { openUrl(ModuleInfo.MODULE_REPO) })
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState, actions: HomeActions) {
    val isDark = isSystemDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.defaultColors(
                color = when {
                    state.moduleEnabled && isDark -> Color(0xFF1A3825)
                    state.moduleEnabled -> Color(0xFFDFFAE4)
                    isDark -> Color(0xFF310808)
                    else -> Color(0xFFF8E2E2)
                }
            ),
            onClick = actions.onStatusClick,
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = if (state.moduleEnabled) {
                            Icons.Rounded.CheckCircleOutline
                        } else {
                            Icons.Rounded.ErrorOutline
                        },
                        tint = if (state.moduleEnabled) {
                                if (isDark) colorScheme.primary.copy(alpha = 0.8f)
                                else Color(0xFF36D167)
                            } else {
                                Color(0xFFF72727)
                            },
                        contentDescription = null
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = if (state.moduleEnabled) "Working" else "Disabled",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = state.moduleVersion,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                insideMargin = PaddingValues(16.dp),
                onClick = actions.onTargetAppsClick,
                showIndication = true,
                pressFeedbackType = PressFeedbackType.Tilt
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Target Apps",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = state.targetPackageCount.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(state: HomeUiState) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            InfoText(title = "Module ID", content = state.moduleId)
            InfoText(title = "Version", content = state.moduleVersion)
            InfoText(
                title = "Config Path",
                content = state.configPath,
                bottomPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun InfoText(
    title: String,
    content: String,
    bottomPadding: Dp = 24.dp,
) {
    Text(
        text = title,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurface
    )
    Text(
        text = content,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
    )
}

@Composable
private fun SourceCodeCard(onOpenUrl: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = "Source Code",
            summary = "View on GitHub",
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = onOpenUrl
        )
    }
}
