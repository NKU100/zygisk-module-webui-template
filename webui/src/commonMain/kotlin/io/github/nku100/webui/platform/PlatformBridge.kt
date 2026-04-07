package io.github.nku100.webui.platform

/**
 * Result of executing a shell command.
 */
data class ShellResult(val errno: Int, val stdout: String, val stderr: String)

/**
 * Represents a package (app) on the device.
 * [iconModel] is platform-specific: a URI string on wasmJs, a Bitmap/Drawable on Android.
 */
data class PackageInfo(
    val packageName: String,
    val label: String = packageName,
    val iconModel: Any? = null,
    val isSystemApp: Boolean = false,
)

/**
 * Whether the current platform is Android (vs wasmJs / browser).
 * Used to conditionally show platform-specific UI (e.g. Blur, FloatingBottomBar).
 */
expect val isAndroidPlatform: Boolean

/**
 * Whether the platform API is available for saving config / executing commands.
 * - Android: always true (runs as Zygisk module WebUI with root)
 * - wasmJs: true only if window.ksu is available
 */
expect fun hasPlatformApi(): Boolean

/**
 * Open a URL in the platform's default browser/handler.
 * - Android: uses Intent.ACTION_VIEW
 * - wasmJs: creates a temporary <a> element to avoid WebView blocking window.open()
 */
expect fun openUrl(url: String)

/**
 * Platform abstraction for KernelSU / Root operations.
 * - wasmJs: bridges to window.ksu via JS interop
 * - Android: executes commands via root shell
 */
expect object PlatformBridge {
    /** Execute a shell command with root privileges. */
    suspend fun exec(command: String): ShellResult

    /** Show a toast message. */
    fun toast(message: String)

    /** List installed packages on the device. */
    suspend fun listPackages(): List<PackageInfo>

    /** Read text content from a file path. */
    suspend fun readFile(path: String): String

    /** Write text content to a file path. */
    suspend fun writeFile(path: String, content: String)
}
