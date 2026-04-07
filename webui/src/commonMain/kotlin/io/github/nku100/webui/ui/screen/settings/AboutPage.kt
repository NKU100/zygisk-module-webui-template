package io.github.nku100.webui.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Policy
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
import io.github.nku100.webui.ModuleInfo
import io.github.nku100.webui.platform.openUrl
import io.github.nku100.webui.ui.util.defaultHazeEffect
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun AboutPage(
    bottomPadding: Dp,
    onBack: () -> Unit,
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
                title = "About",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    top.yukonga.miuix.kmp.basic.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onBackground
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .let { if (enableBlur) it.hazeSource(state = hazeState) else it }
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            // Module Info
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    BasicComponent(
                        title = "Module ID",
                        summary = ModuleInfo.MODULE_ID,
                        startAction = {
                            Icon(
                                Icons.Rounded.Info,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "Module ID",
                                tint = colorScheme.onBackground
                            )
                        },
                    )
                }
            }

            // Author
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    BasicComponent(
                        title = "Author",
                        summary = ModuleInfo.MODULE_AUTHOR,
                        startAction = {
                            Icon(
                                Icons.Rounded.Person,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "Author",
                                tint = colorScheme.onBackground
                            )
                        },
                    )
                }
            }

            // Source Code
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SuperArrow(
                        title = "Source Code",
                        summary = ModuleInfo.MODULE_REPO.removePrefix("https://"),
                        startAction = {
                            Icon(
                                Icons.Rounded.Code,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "Source Code",
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = { openUrl(ModuleInfo.MODULE_REPO) },
                    )
                }
            }

            // License
            item {
                Card(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                ) {
                    BasicComponent(
                        title = "License",
                        summary = "Apache License 2.0",
                        startAction = {
                            Icon(
                                Icons.Rounded.Policy,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = "License",
                                tint = colorScheme.onBackground
                            )
                        },
                    )
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}
