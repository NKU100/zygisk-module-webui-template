package io.github.nku100.webui.platform

import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.js.Promise

// JS interop: bridge to KernelSU's ksu object (v3.0.2 API)
// Official API: https://www.npmjs.com/package/kernelsu

// exec uses callback pattern: ksu.exec(command, options, callbackName)
// KernelSU will call window[callbackName](errno, stdout, stderr)
@JsFun("""
(command, options) => {
    return new Promise((resolve, reject) => {
        const callbackName = '__ksu_exec_' + Date.now() + '_' + Math.random().toString(36).slice(2);
        window[callbackName] = (errno, stdout, stderr) => {
            delete window[callbackName];
            resolve({ errno: errno, stdout: stdout, stderr: stderr });
        };
        try {
            ksu.exec(command, JSON.stringify(options), callbackName);
        } catch (e) {
            delete window[callbackName];
            reject(e);
        }
    });
}
""")
private external fun ksuExecJs(command: String, options: JsAny): Promise<JsAny>

// Empty JS object for default exec options
@JsFun("() => ({})")
private external fun emptyJsObject(): JsAny

// toast: ksu.toast(message) — synchronous
@JsFun("(msg) => ksu.toast(msg)")
private external fun ksuToastJs(message: String)

// listPackages: ksu.listPackages(type) — synchronous, returns JSON string
@JsFun("(type) => { try { return ksu.listPackages(type); } catch(e) { return '[]'; } }")
private external fun ksuListPackagesJs(type: String): String

// moduleInfo: ksu.moduleInfo() — synchronous, returns string
@JsFun("() => ksu.moduleInfo()")
private external fun ksuModuleInfoJs(): JsString

// getPackagesInfo: ksu.getPackagesInfo(packages) — synchronous, returns JSON string
@JsFun("(pkgs) => { try { return ksu.getPackagesInfo(pkgs); } catch(e) { return '[]'; } }")
private external fun ksuGetPackagesInfoJs(packages: String): String

// fullScreen: ksu.fullScreen(isFullScreen)
@JsFun("(v) => ksu.fullScreen(v)")
private external fun ksuFullScreenJs(isFullScreen: Boolean)

// enableEdgeToEdge: ksu.enableEdgeToEdge(enable)
@JsFun("(v) => ksu.enableEdgeToEdge(v)")
private external fun ksuEnableEdgeToEdgeJs(enable: Boolean)

// exit: ksu.exit()
@JsFun("() => ksu.exit()")
private external fun ksuExitJs()

// Utility helpers
@JsFun("(result) => result.errno")
private external fun getErrno(result: JsAny): Int

@JsFun("(result) => result.stdout")
private external fun getStdout(result: JsAny): String

@JsFun("(result) => result.stderr")
private external fun getStderr(result: JsAny): String

@JsFun("(arr) => arr.length")
private external fun arrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun arrayGet(arr: JsAny, index: Int): JsAny

@JsFun("(obj, key) => obj[key] || ''")
private external fun objGetString(obj: JsAny, key: String): String

actual val isAndroidPlatform: Boolean = false

@JsFun("() => typeof window !== 'undefined' && typeof window.ksu !== 'undefined' && window.ksu != null")
private external fun hasKsuApiJs(): Boolean

actual fun hasPlatformApi(): Boolean = hasKsuApiJs()

@JsFun("""(url) => {
    if (typeof window.ksu !== 'undefined' && window.ksu.exec) {
        if (window.ksu.toast) window.ksu.toast('Redirecting to ' + url);
        setTimeout(function() {
            var cb = '__open_url_' + Date.now() + '_' + Math.random().toString(36).slice(2);
            window[cb] = function(errno, stdout, stderr) {
                delete window[cb];
                if (errno !== 0) window.open(url, '_blank');
            };
            try { window.ksu.exec('am start -a android.intent.action.VIEW -d ' + url, '{}', cb); }
            catch(e) { delete window[cb]; window.open(url, '_blank'); }
        }, 100);
    } else {
        window.open(url, '_blank');
    }
}""")
private external fun openUrlJs(url: String)

actual fun openUrl(url: String) = openUrlJs(url)

actual object PlatformBridge {
    actual suspend fun exec(command: String): ShellResult {
        val result = ksuExecJs(command, emptyJsObject()).await<JsAny>()
        return ShellResult(
            errno = getErrno(result),
            stdout = getStdout(result),
            stderr = getStderr(result),
        )
    }

    actual fun toast(message: String) {
        ksuToastJs(message)
    }

    actual suspend fun listPackages(): List<PackageInfo> {
        // Try ksu.listPackages API first (KernelSU manager)
        try {
            val userJson = ksuListPackagesJs("user")
            val systemJson = ksuListPackagesJs("system")
            val userPkgs = if (userJson.isNotBlank() && userJson != "[]")
                Json.parseToJsonElement(userJson).jsonArray.map { it.jsonPrimitive.content }
            else emptyList()
            val systemPkgs = if (systemJson.isNotBlank() && systemJson != "[]")
                Json.parseToJsonElement(systemJson).jsonArray.map { it.jsonPrimitive.content }
            else emptyList()

            if (userPkgs.isNotEmpty() || systemPkgs.isNotEmpty()) {
                val allPkgs = userPkgs + systemPkgs
                val systemSet = systemPkgs.toSet()

                // Try getPackagesInfo for labels
                try {
                    val infoJson = ksuGetPackagesInfoJs(Json.encodeToString(allPkgs))
                    if (infoJson.isNotBlank() && infoJson != "[]") {
                        val infoArray = Json.parseToJsonElement(infoJson).jsonArray
                        return infoArray.map { element ->
                            val obj = element.jsonObject
                            val pkgName = obj["packageName"]?.jsonPrimitive?.content ?: ""
                            val label = obj["appLabel"]?.jsonPrimitive?.content ?: pkgName
                            PackageInfo(
                                packageName = pkgName,
                                label = label.ifBlank { pkgName },
                                iconModel = "ksu://icon/$pkgName",
                                isSystemApp = pkgName in systemSet,
                            )
                        }
                    }
                } catch (_: Exception) { /* getPackagesInfo not available */ }

                return allPkgs.map {
                    PackageInfo(
                        packageName = it,
                        iconModel = "ksu://icon/$it",
                        isSystemApp = it in systemSet,
                    )
                }
            }
        } catch (_: Exception) { /* listPackages not available (e.g. KsuWebUIStandalone) */ }

        // Fallback: use exec("pm list packages -3")
        return try {
            val result = exec("pm list packages -3")
            if (result.errno != 0) return emptyList()
            result.stdout.lines()
                .filter { it.startsWith("package:") }
                .map { PackageInfo(packageName = it.removePrefix("package:").trim()) }
                .filter { it.packageName.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual suspend fun readFile(path: String): String {
        val result = exec("cat '$path' 2>/dev/null || echo ''")
        return result.stdout
    }

    actual suspend fun writeFile(path: String, content: String) {
        val escaped = content.replace("'", "'\\''")
        exec("echo '$escaped' > '$path'")
    }
}
