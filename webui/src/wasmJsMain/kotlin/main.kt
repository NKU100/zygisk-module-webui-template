import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.window.ComposeViewport
import io.github.nku100.webui.ui.App
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import kotlin.js.Promise
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * CJK font path served from webroot.
 * post-fs-data.sh symlinks /system/fonts/NotoSansCJK-Regular.ttc into webroot/.
 * If the file doesn't exist (e.g. device has no CJK font), we skip font loading
 * and fall back to Skia's built-in Latin-only font.
 */
private const val CJK_FONT_URL = "./NotoSansCJK-Regular.ttc"

// ── JS helpers for ArrayBuffer → ByteArray conversion ────────────────────────

@JsFun("(url) => fetch(url).then(r => { if (!r.ok) throw new Error(r.status); return r.arrayBuffer(); })")
private external fun fetchArrayBuffer(url: String): Promise<JsAny>

@JsFun("(buf) => new Int8Array(buf)")
private external fun toInt8Array(buf: JsAny): Int8Array

@JsFun("(src, size, dstAddr) => { const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size); mem8.set(src); }")
private external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

@JsFun("(arr) => arr.length")
private external fun int8ArrayLength(arr: Int8Array): Int

@OptIn(UnsafeWasmMemoryApi::class)
private fun Int8Array.toByteArray(): ByteArray {
    val size = int8ArrayLength(this)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(this@toByteArray, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}

private suspend fun fetchFontBytes(url: String): ByteArray? {
    return try {
        val buf = fetchArrayBuffer(url).await<JsAny>()
        toInt8Array(buf).toByteArray()
    } catch (_: Throwable) {
        null
    }
}

// ── Entry point ──────────────────────────────────────────────────────────────

fun main() {
    val body = document.body ?: return
    ComposeViewport(body) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontsReady by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val bytes = fetchFontBytes(CJK_FONT_URL)
            if (bytes != null && bytes.isNotEmpty()) {
                val fontFamily = FontFamily(Font("NotoSansCJK", bytes))
                fontFamilyResolver.preload(fontFamily)
            }
            fontsReady = true
        }

        if (fontsReady) {
            App()
        }
    }
}
