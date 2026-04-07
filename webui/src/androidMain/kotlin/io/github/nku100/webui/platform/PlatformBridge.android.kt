package io.github.nku100.webui.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual val isAndroidPlatform: Boolean = true

actual fun hasPlatformApi(): Boolean = true

actual fun openUrl(url: String) {
    val context = PlatformBridge.appContext ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

actual object PlatformBridge {
    // Set by the Activity on creation
    var appContext: Context? = null
    var toastCallback: ((String) -> Unit)? = null

    actual suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val errno = process.waitFor()
            ShellResult(errno, stdout, stderr)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "Unknown error")
        }
    }

    actual fun toast(message: String) {
        toastCallback?.invoke(message)
    }

    actual suspend fun listPackages(): List<PackageInfo> = withContext(Dispatchers.IO) {
        val ctx = appContext
        if (ctx != null) {
            // Use PackageManager for label + icon
            val pm = ctx.packageManager
            pm.getInstalledApplications(0)
                .map { info ->
                    PackageInfo(
                        packageName = info.packageName,
                        label = info.loadLabel(pm).toString(),
                        iconModel = info.loadIcon(pm),
                        isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .sortedBy { it.label.lowercase() }
        } else {
            // Fallback: shell command
            val result = exec("pm list packages")
            if (result.errno != 0) return@withContext emptyList()
            result.stdout.lines()
                .filter { it.startsWith("package:") }
                .map { PackageInfo(packageName = it.removePrefix("package:").trim()) }
        }
    }

    actual suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        try {
            val result = exec("cat '$path'")
            if (result.errno == 0) result.stdout else ""
        } catch (_: Exception) { "" }
    }

    actual suspend fun writeFile(path: String, content: String) {
        withContext(Dispatchers.IO) {
            val escaped = content.replace("'", "'\\''")
            exec("echo '$escaped' > '$path'")
        }
    }
}
