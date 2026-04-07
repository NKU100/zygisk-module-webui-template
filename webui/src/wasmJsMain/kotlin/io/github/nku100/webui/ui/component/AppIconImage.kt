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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Image as SkiaImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

// XHR supports custom URL schemes intercepted by WebView's shouldInterceptRequest.
// Requires shouldInterceptRequest to return Access-Control-Allow-Origin: * header.
// fetch() does NOT support ksu:// scheme in WebView.
// Uses FileReader.readAsDataURL for efficient binary→base64 conversion (no per-byte JS loop).
@JsFun("""
(url) => new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'blob';
    xhr.onload = () => {
        if (xhr.status === 200 || xhr.status === 0) {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result.split(',')[1]);
            reader.onerror = () => reject(new Error('FileReader error'));
            reader.readAsDataURL(xhr.response);
        } else {
            reject(new Error('XHR failed: ' + xhr.status));
        }
    };
    xhr.onerror = () => reject(new Error('XHR error'));
    xhr.send();
})
""")
private external fun xhrAsBase64(url: String): Promise<JsString>

private val iconCache = mutableMapOf<String, ImageBitmap>()
private val loadSemaphore = Semaphore(4)

@OptIn(ExperimentalEncodingApi::class)
private fun skiaImageToImageBitmap(image: SkiaImage): ImageBitmap = image.toComposeImageBitmap()

@OptIn(ExperimentalEncodingApi::class)
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
                val bmp = loadSemaphore.withPermit {
                    val base64 = xhrAsBase64(url).await<JsString>().toString()
                    val bytes = Base64.decode(base64)
                    skiaImageToImageBitmap(SkiaImage.makeFromEncoded(bytes))
                }
                iconCache[url] = bmp
                bitmap = bmp
            } catch (_: Throwable) { }
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
