package io.github.nku100.webui.ui.component

import android.content.pm.ApplicationInfo
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nku100.webui.ui.LetterIcon
import io.github.nku100.webui.ui.util.AppIconCache
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class IconKey(val uid: Int, val packageName: String)

@Composable
actual fun AppIconImage(
    iconModel: Any?,
    packageName: String,
    contentDescription: String,
    modifier: Modifier,
    size: Dp,
) {
    val applicationInfo = iconModel as? ApplicationInfo
    if (applicationInfo == null) {
        LetterIcon(packageName = packageName, modifier = modifier.size(size))
        return
    }

    val density = LocalDensity.current
    val context = LocalContext.current
    val targetSizePx = with(density) { size.roundToPx() }

    val iconKey = IconKey(applicationInfo.uid, applicationInfo.packageName)
    val cachedBitmap = remember(iconKey) {
        AppIconCache.getFromCache(applicationInfo)
    }

    Box(modifier = modifier.size(size)) {
        var appBitmap by remember(iconKey) { mutableStateOf(cachedBitmap) }

        if (cachedBitmap == null) {
            LaunchedEffect(iconKey) {
                appBitmap = AppIconCache.loadIcon(context, applicationInfo, targetSizePx)
            }
        }

        if (cachedBitmap != null) {
            val imageBitmap = remember(appBitmap) { appBitmap!!.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Crossfade(
                targetState = appBitmap,
                animationSpec = tween(durationMillis = 150),
                label = "IconFade",
            ) { icon ->
                if (icon == null) {
                    PlaceHolderBox(Modifier.fillMaxSize())
                } else {
                    val imageBitmap = remember(icon) { icon.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceHolderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
    )
}
