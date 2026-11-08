package io.github.nku100.webui.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nku100.webui.ui.theme.ThemeMode
import io.github.nku100.webui.ui.util.rememberDefaultBlurBackdrop
import io.github.nku100.webui.ui.util.topBarDefaultWindowInsetsPadding
import io.github.nku100.webui.ui.util.topBarModifier
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import io.github.nku100.webui.generated.resources.Res
import io.github.nku100.webui.generated.resources.back
import io.github.nku100.webui.generated.resources.bottom_bar_glass_effect
import io.github.nku100.webui.generated.resources.bottom_bar_glass_effect_summary
import io.github.nku100.webui.generated.resources.dynamic_colors
import io.github.nku100.webui.generated.resources.dynamic_colors_summary
import io.github.nku100.webui.generated.resources.floating_bottom_bar
import io.github.nku100.webui.generated.resources.floating_bottom_bar_summary
import io.github.nku100.webui.generated.resources.blur_effects
import io.github.nku100.webui.generated.resources.blur_effects_summary
import io.github.nku100.webui.generated.resources.navigation_badge
import io.github.nku100.webui.generated.resources.navigation_badge_summary
import io.github.nku100.webui.generated.resources.page_scale
import io.github.nku100.webui.generated.resources.page_scale_summary
import io.github.nku100.webui.generated.resources.theme_dark
import io.github.nku100.webui.generated.resources.theme_follow_system
import io.github.nku100.webui.generated.resources.theme_light
import io.github.nku100.webui.generated.resources.theme_mode
import io.github.nku100.webui.generated.resources.theme_mode_summary
import io.github.nku100.webui.generated.resources.theme_settings
import kotlin.math.roundToInt

@Composable
fun ThemeSettingsPage(
    uiState: ThemeSettingsUiState,
    actions: ThemeSettingsActions,
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
                title = stringResource(Res.string.theme_settings),
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = topBarDefaultWindowInsetsPadding,
                navigationIcon = {
                    top.yukonga.miuix.kmp.basic.IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = colorScheme.onBackground,
                        )
                    }
                },
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
                    OverlayDropdownPreference(
                        title = stringResource(Res.string.theme_mode),
                        summary = stringResource(Res.string.theme_mode_summary),
                        items = themeModeItems,
                        startAction = {
                            Icon(
                                Icons.Rounded.Palette,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.theme_mode),
                                tint = colorScheme.onBackground,
                            )
                        },
                        selectedIndex = ThemeMode.entries.indexOf(uiState.themeMode),
                        onSelectedIndexChange = { actions.onThemeModeChange(ThemeMode.entries[it]) },
                    )
                    SwitchPreference(
                        title = stringResource(Res.string.dynamic_colors),
                        summary = stringResource(Res.string.dynamic_colors_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.dynamic_colors),
                                tint = colorScheme.onBackground,
                            )
                        },
                        checked = uiState.enableMonet,
                        onCheckedChange = actions.onEnableMonetChange,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = stringResource(Res.string.blur_effects),
                        summary = stringResource(Res.string.blur_effects_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.BlurOn,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.blur_effects),
                                tint = colorScheme.onBackground,
                            )
                        },
                        checked = uiState.enableBlur,
                        onCheckedChange = actions.onEnableBlurChange,
                    )
                    SwitchPreference(
                        title = stringResource(Res.string.floating_bottom_bar),
                        summary = stringResource(Res.string.floating_bottom_bar_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.CallToAction,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.floating_bottom_bar),
                                tint = colorScheme.onBackground,
                            )
                        },
                        checked = uiState.enableFloatingBottomBar,
                        onCheckedChange = actions.onEnableFloatingBottomBarChange,
                    )
                    AnimatedVisibility(visible = uiState.enableFloatingBottomBar) {
                        SwitchPreference(
                            title = stringResource(Res.string.bottom_bar_glass_effect),
                            summary = stringResource(Res.string.bottom_bar_glass_effect_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.WaterDrop,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(Res.string.bottom_bar_glass_effect),
                                    tint = colorScheme.onBackground,
                                )
                            },
                            checked = uiState.enableFloatingBottomBarBlur,
                            onCheckedChange = actions.onEnableFloatingBottomBarBlurChange,
                        )
                    }
                    SwitchPreference(
                        title = stringResource(Res.string.navigation_badge),
                        summary = stringResource(Res.string.navigation_badge_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.Pin,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(Res.string.navigation_badge),
                                tint = colorScheme.onBackground,
                            )
                        },
                        checked = uiState.enableNavigationBadge,
                        onCheckedChange = actions.onNavigationBadgeChange,
                    )
                }
            }

            item {
                var sliderValue by remember(uiState.pageScale) {
                    mutableFloatStateOf(uiState.pageScale.coerceIn(0.8f, 1.1f))
                }
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.AspectRatio,
                            modifier = Modifier.padding(end = 12.dp),
                            contentDescription = stringResource(Res.string.page_scale),
                            tint = colorScheme.onBackground,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.page_scale),
                                        color = colorScheme.onBackground,
                                    )
                                    Text(
                                        text = stringResource(Res.string.page_scale_summary),
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                                Text(
                                    text = "${(sliderValue * 100).roundToInt()}%",
                                    color = colorScheme.onSurfaceVariantActions,
                                )
                            }
                            Slider(
                                modifier = Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val up = waitForUpOrCancellation()
                                        if (up != null &&
                                            (up.position - down.position).getDistance() <= viewConfiguration.touchSlop
                                        ) {
                                            val selectedScale = pageScaleForTap(
                                                positionX = up.position.x,
                                                width = size.width,
                                                height = size.height,
                                            )
                                            sliderValue = selectedScale
                                            actions.onPageScaleChange(selectedScale)
                                        }
                                    }
                                },
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { actions.onPageScaleChange(sliderValue) },
                                valueRange = 0.8f..1.1f,
                                showKeyPoints = true,
                                keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                magnetThreshold = 0.01f,
                                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(bottomPadding)) }
        }
    }
}

internal fun pageScaleForTap(positionX: Float, width: Int, height: Int): Float {
    val thumbRadius = height / 2f
    val availableWidth = (width - 2f * thumbRadius).coerceAtLeast(0f)
    val fraction = if (availableWidth == 0f) {
        0f
    } else {
        ((positionX - thumbRadius) / availableWidth).coerceIn(0f, 1f)
    }
    val keyPointFractions = floatArrayOf(0f, 1f / 3f, 2f / 3f, 1f)
    val nearestKeyPoint = keyPointFractions.minBy { kotlin.math.abs(it - fraction) }
    val resolvedFraction = if (kotlin.math.abs(nearestKeyPoint - fraction) < 0.01f) {
        nearestKeyPoint
    } else {
        fraction
    }
    return 0.8f + (1.1f - 0.8f) * resolvedFraction
}
