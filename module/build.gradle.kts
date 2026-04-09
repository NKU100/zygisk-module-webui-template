import android.databinding.tool.ext.capitalizeUS
import groovy.json.JsonBuilder
import org.apache.commons.codec.binary.Hex
import org.apache.tools.ant.filters.FixCrLfFilter
import org.apache.tools.ant.filters.ReplaceTokens
import java.security.MessageDigest

plugins {
    alias(libs.plugins.agp.app)
}

// All git commands use Provider-based lazy evaluation (configuration-cache friendly).
// Values are only resolved when a task actually needs them, not during configuration.
val commitCount = providers.exec { commandLine("git", "rev-list", "HEAD", "--count") }
    .standardOutput.asText.map { it.trim().toInt() }
val commitHash = providers.exec { commandLine("git", "rev-parse", "--verify", "--short", "HEAD") }
    .standardOutput.asText.map { it.trim() }
val tag = providers.exec {
    commandLine("git", "tag", "--points-at", "HEAD", "--sort=-v:refname")
    isIgnoreExitValue = true
}.standardOutput.asText.map { text ->
    // Pick the first v* tag on HEAD; fall back to "ci" for untagged commits
    text.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("v") }.orEmpty()
        .ifEmpty { "ci" }
}

val remoteUrl = providers.exec { commandLine("git", "remote", "get-url", "origin") }
    .standardOutput.asText.map { it.trim() }
val gitHubUser = remoteUrl.map { url ->
    val match = Regex("""(?:https://github\.com/|git@github\.com:)([^/]+)/([^/]+?)(?:\.git)?$""").find(url)
    match?.groupValues?.get(1) ?: "NKU100"
}
val gitHubRepo = remoteUrl.map { url ->
    val match = Regex("""(?:https://github\.com/|git@github\.com:)([^/]+)/([^/]+?)(?:\.git)?$""").find(url)
    match?.groupValues?.get(2) ?: "zygisk-module-webui-template"
}

// Resolve extra properties eagerly into plain vals (not delegated properties).
// `by rootProject.extra` delegates capture rootProject, which cannot be
// serialized/deserialized by the Configuration Cache.
val moduleId = rootProject.extra["moduleId"] as String
val moduleName = rootProject.extra["moduleName"] as String
val moduleAuthor = rootProject.extra["moduleAuthor"] as String
val moduleDesc = rootProject.extra["moduleDesc"] as String
val moduleLibName = rootProject.extra["moduleLibName"] as String
@Suppress("UNCHECKED_CAST")
val abiList = rootProject.extra["abiList"] as List<String>

android {
    defaultConfig {
        ndk {
            abiFilters.addAll(abiList)
        }
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++20")
                arguments(
                    "-DANDROID_STL=none", "-DMODULE_NAME=$moduleLibName",
                    "-DCMAKE_C_COMPILER_LAUNCHER=ccache",
                    "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache",
                )
            }
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
}

androidComponents.onVariants { variant ->
    val variantLowered = variant.name.lowercase()
    val variantCapped = variant.name.capitalizeUS()
    val buildTypeLowered = variant.buildType?.lowercase() ?: "debug"
    val supportedAbis = abiList.joinToString(" ") {
        when (it) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86" -> "x86"
            "x86_64" -> "x64"
            else -> error("unsupported abi $it")
        }
    }

    // Use .dir() for directories (not .file())
    val moduleDir = layout.buildDirectory.dir("outputs/module/$variantLowered")
    // Derived values resolved lazily via Provider.map
    val zipFileName = commitCount.zip(commitHash) { count, hash ->
        "$moduleName-${tag.get()}-$count-$hash-$buildTypeLowered.zip".replace(' ', '-')
    }
    val versionName = commitCount.zip(commitHash) { count, hash ->
        "${tag.get()} ($count-$hash-$variantLowered)"
    }
    val versionCode = commitCount

    val prepareModuleFilesTask = tasks.register<Sync>("prepareModuleFiles$variantCapped") {
        group = "module"
        dependsOn("assemble$variantCapped", ":webui:buildWebUI")
        into(moduleDir)
        // Declare inputs so Gradle invalidates cache when values change
        inputs.property("moduleId", moduleId)
        inputs.property("moduleName", moduleName)
        inputs.property("versionName", versionName)
        inputs.property("versionCode", versionCode)
        inputs.property("buildType", buildTypeLowered)
        from(rootProject.layout.projectDirectory.dir("webui/build/dist/wasmJs/productionExecutable")) {
            into("webroot")
        }
        from(rootProject.layout.projectDirectory.file("README.md"))
        from(layout.projectDirectory.file("template")) {
            exclude("module.prop", "customize.sh", "post-fs-data.sh", "service.sh")
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
        }
        // Stable (tagged release): point to latest so older versions discover newer releases.
        // Beta (CI build): point to the ci pre-release tag.
        val updateJson = gitHubUser.zip(gitHubRepo) { user, repo ->
            if (tag.get().startsWith("v")) {
                "https://github.com/$user/$repo/releases/latest/download/update.json"
            } else {
                "https://github.com/$user/$repo/releases/download/ci/update.json"
            }
        }
        from(layout.projectDirectory.file("template")) {
            include("module.prop")
            expand(
                "moduleId" to moduleId,
                "moduleName" to moduleName,
                "versionName" to versionName.get(),
                "versionCode" to versionCode.get(),
                "moduleAuthor" to moduleAuthor,
                "moduleDesc" to moduleDesc,
                "updateJson" to updateJson.get(),
            )
        }
        from(layout.projectDirectory.file("template")) {
            include("customize.sh", "post-fs-data.sh", "service.sh")
            val tokens = mapOf(
                "DEBUG" to if (buildTypeLowered == "debug") "true" else "false",
                "SONAME" to moduleLibName,
                "SUPPORTED_ABIS" to supportedAbis,
                "MODULE_ID" to moduleId,
            )
            filter<ReplaceTokens>("tokens" to tokens)
            filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
        }
        from(layout.buildDirectory.file("intermediates/stripped_native_libs/$variantLowered/strip${variantCapped}DebugSymbols/out/lib")) {
            into("lib")
        }

        doLast {
            // Use destinationDir (Sync task property) + plain Java IO instead of
            // project.fileTree / project.file to avoid capturing the script object,
            // which is required for Configuration Cache compatibility.
            destinationDir.walkTopDown()
                .filter { it.isFile && !it.name.endsWith(".sha256") }
                .forEach { f ->
                    val md = MessageDigest.getInstance("SHA-256")
                    f.forEachBlock(4096) { bytes, size -> md.update(bytes, 0, size) }
                    File(f.path + ".sha256").writeText(Hex.encodeHexString(md.digest()))
                }
        }
    }

    val zipTask = tasks.register<Zip>("zip$variantCapped") {
        group = "module"
        dependsOn(prepareModuleFilesTask)
        archiveFileName.set(zipFileName)
        destinationDirectory.set(layout.projectDirectory.file("release").asFile)
        from(moduleDir)
        // Declare inputs for CC compatibility
        inputs.property("zipFileName", zipFileName)
        inputs.property("buildType", buildTypeLowered)
        // Clean stale zips of the same variant to avoid confusion
        doFirst {
            val props = inputs.properties
            val name = props["zipFileName"] as String
            val btype = props["buildType"] as String
            destinationDirectory.get().asFile.listFiles()?.filter {
                it.name.endsWith("-$btype.zip") && it.name != name
            }?.forEach { it.delete() }
        }
    }

    // Resolve layout.projectDirectory eagerly before the task closure
    val releaseDir = layout.projectDirectory.file("release/update.json").asFile

    tasks.register("ci$variantCapped") {
        group = "module"
        dependsOn(zipTask)
        inputs.property("zipFileName", zipFileName)
        inputs.property("versionName", versionName)
        inputs.property("versionCode", versionCode)
        inputs.property("gitHubUser", gitHubUser)
        inputs.property("gitHubRepo", gitHubRepo)
        inputs.property("tag", tag)

        doLast {
            val props = inputs.properties
            val name = props["zipFileName"] as String
            val version = props["versionName"] as String
            val code = props["versionCode"] as Int
            val user = props["gitHubUser"] as String
            val repo = props["gitHubRepo"] as String
            val t = props["tag"] as String
            val jsonContent = mapOf(
                "version" to version,
                "versionCode" to code,
                "zipUrl" to "https://github.com/$user/$repo/releases/download/$t/$name",
                "changelog" to "https://github.com/$user/$repo/releases/download/$t/changelog.md"
            )
            releaseDir.writeText(JsonBuilder(jsonContent).toPrettyString())
        }
    }

    val pushTask = tasks.register<Exec>("push$variantCapped") {
        group = "module"
        dependsOn(zipTask)
        inputs.property("zipFileName", zipFileName)
        doFirst {
            val zipFile = zipTask.get().outputs.files.singleFile
            commandLine("adb", "push", zipFile.path, "/data/local/tmp")
        }
    }

    val installKsuTask = tasks.register<Exec>("installKsu$variantCapped") {
        group = "module"
        dependsOn(pushTask)
        inputs.property("zipFileName", zipFileName)
        doFirst {
            val name = inputs.properties["zipFileName"] as String
            commandLine(
                "adb", "shell", "su", "-c",
                "/data/adb/ksud module install /data/local/tmp/$name"
            )
        }
    }

    val installMagiskTask = tasks.register<Exec>("installMagisk$variantCapped") {
        group = "module"
        dependsOn(pushTask)
        inputs.property("zipFileName", zipFileName)
        doFirst {
            val name = inputs.properties["zipFileName"] as String
            commandLine(
                "adb", "shell", "su", "-M", "-c",
                "magisk --install-module /data/local/tmp/$name"
            )
        }
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
