package io.github.nku100.webui.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.nku100.webui.ui.LetterIcon
import kotlinx.coroutines.await
import org.jetbrains.skia.Image as SkiaImage
import kotlin.js.Promise

@JsFun("(url) => fetch(url).then(r => r.arrayBuffer())")
private external fun fetchArrayBuffer(url: String): Promise<JsAny>

@JsFun("(buf) => new Uint8Array(buf)")
private external fun toUint8Array(buf: JsAny): JsAny

@JsFun("(arr) => arr.length")
private external fun arrLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun arrGet(arr: JsAny, i: Int): Int

private val iconCache = mutableMapOf<String, ImageBitmap>()

private fun skiaImageToImageBitmap(image: SkiaImage): ImageBitmap = image.toComposeImageBitmap()

@Composable
actual fun AppIconImage(
    iconModel: Any?,
    packageName: String,
    contentDescription: String,
    modifier: Modifier,
    size: Dp,
) {
    val url = iconModel as? String

    var bitmap by remember(url) { mutableStateOf(url?.let { iconCache[it] }) }

    if (url != null && bitmap == null) {
        LaunchedEffect(url) {
            val cached = iconCache[url]
            if (cached != null) { bitmap = cached; return@LaunchedEffect }
            try {
                val buf = fetchArrayBuffer(url).await<JsAny>()
                val arr = toUint8Array(buf)
                val len = arrLength(arr)
                val bytes = ByteArray(len) { arrGet(arr, it).toByte() }
                val bmp = skiaImageToImageBitmap(SkiaImage.makeFromEncoded(bytes))
                iconCache[url] = bmp
                bitmap = bmp
            } catch (_: Exception) { }
        }
    }

    Box(modifier = modifier.size(size)) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bitmap = bmp, contentDescription = contentDescription, modifier = Modifier.fillMaxSize())
        } else {
            LetterIcon(packageName = packageName, modifier = Modifier.fillMaxSize())
        }
    }
}
