import android.databinding.tool.ext.capitalizeUS
import groovy.json.JsonBuilder
import org.apache.commons.codec.binary.Hex
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.kotlin.dsl.register
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.agp.app)
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = ByteArrayOutputStream()
    val byteErr = ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
        errorOutput = byteErr
    }
    if (byteErr.size() > 0) {
        throw Exception(byteErr.toString())
    }
    return String(byteOut.toByteArray()).trim()
}

val commitCount = "git rev-list HEAD --count".execute().toInt()
val commitHash = "git rev-parse --verify --short HEAD".execute()
val tag = try {
    "git describe --tags --abbrev=0".execute()
} catch (e: Exception) {
    println("Failed to get tag: $e")
    "ci"
}
val remoteUrl = "git remote get-url origin".execute()
var gitHubUser = "NKU100"
var gitHubRepo = "zygisk-module-webui-template"
try {
    val regex = Regex("""(?:https://github\\.com/|git@github\\.com:)([^/]+)/([^/]+?)(?:\\.git)?$""")
    val matchResult = regex.find(remoteUrl)
    if (matchResult != null) {
        gitHubUser = matchResult.groupValues[1]
        gitHubRepo = matchResult.groupValues[2]
    }
} catch (_: Exception) {
    println("Failed to parse remote url: $remoteUrl")
}

val moduleId: String by rootProject.extra
val moduleName: String by rootProject.extra
val moduleAuthor: String by rootProject.extra
val moduleDesc: String by rootProject.extra
val moduleLibName: String by rootProject.extra
val abiList: List<String> by rootProject.extra

android {
    defaultConfig {
        ndk {
            abiFilters.addAll(abiList)
        }
        externalNativeBuild {
            /*
            ndkBuild {
                arguments("MODULE_NAME=$moduleId")
            }
            */
            cmake {
                cppFlags("-std=c++20")
                arguments(
                    "-DANDROID_STL=none", "-DMODULE_NAME=$moduleLibName"
                )
            }
        }
    }
    externalNativeBuild {
        /*
        ndkBuild {
            path("src/main/cpp/Android.mk")
        }
        */
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
}

androidComponents.onVariants { variant ->
    afterEvaluate {
        val variantLowered = variant.name.lowercase()
        val variantCapped = variant.name.capitalizeUS()
        val buildTypeLowered = variant.buildType?.lowercase()
        val supportedAbis = abiList.joinToString(" ") {
            when (it) {
                "arm64-v8a" -> "arm64"
                "armeabi-v7a" -> "arm"
                "x86" -> "x86"
                "x86_64" -> "x64"
                else -> error("unsupported abi $it")
            }
        }

        val moduleDir = layout.buildDirectory.file("outputs/module/$variantLowered")
        val zipFileName =
            "$moduleName-$tag-$commitCount-$commitHash-$buildTypeLowered.zip".replace(' ', '-')
        val versionName = "$tag ($commitCount-$commitHash-$variantLowered)"
        val versionCode = commitCount

        val prepareModuleFilesTask = tasks.register<Sync>("prepareModuleFiles$variantCapped") {
            group = "module"
            dependsOn("assemble$variantCapped")
            into(moduleDir)
            from(rootProject.layout.projectDirectory.file("README.md"))
            from(layout.projectDirectory.file("template")) {
                exclude("module.prop", "customize.sh", "post-fs-data.sh", "service.sh")
                filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
            }
            val updateJson =
                "https://github.com/$gitHubUser/$gitHubRepo/releases/download/$tag/update.json"
            from(layout.projectDirectory.file("template")) {
                include("module.prop")
                expand(
                    "moduleId" to moduleId,
                    "moduleName" to moduleName,
                    "versionName" to versionName,
                    "versionCode" to versionCode,
                    "moduleAuthor" to moduleAuthor,
                    "moduleDesc" to moduleDesc,
                    "updateJson" to updateJson,
                )
            }
            from(layout.projectDirectory.file("template")) {
                include("customize.sh", "post-fs-data.sh", "service.sh")
                val tokens = mapOf(
                    "DEBUG" to if (buildTypeLowered == "debug") "true" else "false",
                    "SONAME" to moduleLibName,
                    "SUPPORTED_ABIS" to supportedAbis
                )
                filter<ReplaceTokens>("tokens" to tokens)
                filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
            }
            from(layout.buildDirectory.file("intermediates/stripped_native_libs/$variantLowered/strip${variantCapped}DebugSymbols/out/lib")) {
                into("lib")
            }

            doLast {
                fileTree(moduleDir).visit {
                    if (isDirectory) return@visit
                    val md = MessageDigest.getInstance("SHA-256")
                    file.forEachBlock(4096) { bytes, size ->
                        md.update(bytes, 0, size)
                    }
                    file(file.path + ".sha256").writeText(
                        Hex.encodeHexString(
                            md.digest()
                        )
                    )
                }
            }
        }

        val zipTask = tasks.register<Zip>("zip$variantCapped") {
            group = "module"
            dependsOn(prepareModuleFilesTask)
            archiveFileName.set(zipFileName)
            destinationDirectory.set(layout.projectDirectory.file("release").asFile)
            from(moduleDir)
        }

        tasks.register("ci$variantCapped") {
            group = "module"
            dependsOn(zipTask)

            doLast {
                val updateJsonFile = layout.projectDirectory.file("release/update.json").asFile
                val jsonContent = mapOf(
                    "version" to versionName,
                    "versionCode" to versionCode,
                    "zipUrl" to "https://github.com/$gitHubUser/$gitHubRepo/releases/download/$tag/$zipFileName",
                    "changelog" to "https://github.com/$gitHubUser/$gitHubRepo/releases/download/$tag/changelog.md"
                )
                updateJsonFile.writeText(JsonBuilder(jsonContent).toPrettyString())
            }
        }

        val pushTask = tasks.register<Exec>("push$variantCapped") {
            group = "module"
            dependsOn(zipTask)
            commandLine(
                "adb", "push", zipTask.get().outputs.files.singleFile.path, "/data/local/tmp"
            )
        }

        val installKsuTask = tasks.register<Exec>("installKsu$variantCapped") {
            group = "module"
            dependsOn(pushTask)
            commandLine(
                "adb",
                "shell",
                "su",
                "-c",
                "/data/adb/ksud module install /data/local/tmp/$zipFileName"
            )
        }

        val installMagiskTask = tasks.register<Exec>("installMagisk$variantCapped") {
            group = "module"
            dependsOn(pushTask)
            commandLine(
                "adb",
                "shell",
                "su",
                "-M",
                "-c",
                "magisk --install-module /data/local/tmp/$zipFileName"
            )
        }

        tasks.register<Exec>("installKsuAndReboot$variantCapped") {
            group = "module"
            dependsOn(installKsuTask)
            commandLine("adb", "reboot")
        }

        tasks.register<Exec>("installMagiskAndReboot$variantCapped") {
            group = "module"
            dependsOn(installMagiskTask)
            commandLine("adb", "reboot")
        }
    }
}
