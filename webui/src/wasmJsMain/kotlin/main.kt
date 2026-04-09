import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import io.github.nku100.webui.ui.App
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import zygisk_module_webui_template.webui.generated.resources.Res

/**
 * CJK font bundled via Compose Resources (composeResources/font/).
 * We read the raw bytes and register via Font(identity, bytes) so that
 * Skia/CanvasKit picks it up as a fallback for CJK glyphs.
 */
private const val CJK_FONT_RES_PATH = "font/noto_sans_sc_regular.woff2"

@OptIn(ExperimentalResourceApi::class)
fun main() {
    val body = document.body ?: return
    ComposeViewport(body) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontsReady by remember { mutableStateOf(false) }

        // Load CJK font bytes from Compose Resources, register with Skia
        // via platform Font so it serves as a CJK fallback. App renders
        // only after the font is ready, avoiding tofu glyph flash.
        LaunchedEffect(Unit) {
            try {
                val bytes = Res.readBytes(CJK_FONT_RES_PATH)
                if (bytes.isNotEmpty()) {
                    val fontFamily = FontFamily(Font("NotoSansSC", bytes))
                    fontFamilyResolver.preload(fontFamily)
                }
            } catch (_: Throwable) {
                // Font loading failed; fall back to built-in Latin font.
            }
            fontsReady = true
        }

        if (fontsReady) {
            App()
        }
    }
}
