package io.github.nku100.webui.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

@Composable
actual fun statusBarTopPadding(): Dp {
    return WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
}

@Composable
actual fun navigationBarBottomPadding(): Dp {
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}
