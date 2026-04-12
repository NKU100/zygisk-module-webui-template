plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.android.kotlin.multiplatform.library")
}

// Resolve extra properties eagerly into plain vals (not delegated properties).
// `by rootProject.extra` delegates capture rootProject, which cannot be
// serialized/deserialized by the Configuration Cache.
val moduleId = rootProject.extra["moduleId"] as String
val moduleRepo = rootProject.extra["moduleRepo"] as String
val androidCompileSdkVersion = rootProject.extra["androidCompileSdkVersion"] as Int
val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "webui.js"
            }
            runTask {
                mainOutputFileName.set("webui.js")
                devServerProperty.set(
                    devServerProperty.get().copy(open = false)
                )
            }
        }
        binaries.executable()
    }

    androidLibrary {
        namespace = "io.github.nku100.webui"
        compileSdk = androidCompileSdkVersion
        minSdk = androidMinSdkVersion
        androidResources {
            enable = true
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.js.ExperimentalWasmJsInterop",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation("com.kyant.capsule:capsule")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(compose.materialIconsExtended)
            implementation(libs.activity.compose)
            implementation(libs.miuix.android)
            implementation(libs.miuix.icons.android)
            implementation("com.kyant.backdrop:backdrop")
            implementation(libs.haze.android)
            implementation(libs.material.kolor)
            implementation(libs.appiconloader)
        }
        wasmJsMain.dependencies {
            implementation(compose.materialIconsExtended)
            // KMP main artifacts — Gradle auto-resolves to wasmJs variant
            implementation(libs.miuix)
            implementation(libs.miuix.icons)
            implementation("com.kyant.backdrop:backdrop")
            implementation(libs.haze)
            implementation(libs.material.kolor)
        }
    }
}

// Generate ModuleInfo.kt — only compile-time constants that cannot be read at runtime.
// Dynamic metadata (name, version, author) is read from module.prop at runtime.
val generateModuleInfo = tasks.register("generateModuleInfo") {
    val outputDir = layout.buildDirectory.dir("generated/moduleInfo/commonMain/kotlin")
    val pkg = "io.github.nku100.webui"
    inputs.property("moduleId", moduleId)
    inputs.property("moduleRepo", moduleRepo)
    outputs.dir(outputDir)
    doLast {
        val props = inputs.properties
        val id = props["moduleId"] as String
        val repo = props["moduleRepo"] as String

        val dir = outputDir.get().asFile.resolve(pkg.replace('.', '/'))
        dir.mkdirs()
        dir.resolve("ModuleInfo.kt").writeText(
            """
            |package $pkg
            |
            |/** Auto-generated from module.gradle.kts — do not edit. */
            |object ModuleInfo {
            |    const val MODULE_ID = "$id"
            |    const val MODULE_REPO = "$repo"
            |    const val CONFIG_PATH = "/data/adb/$id/config.json"
            |    const val MODULE_PROP_PATH = "/data/adb/modules/$id/module.prop"
            |}
            """.trimMargin()
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(generateModuleInfo.map { layout.buildDirectory.dir("generated/moduleInfo/commonMain/kotlin") })
}

// WebUI build tasks for module integration
val webDistDir = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")

tasks.register("buildWebUI") {
    group = "webui"
    dependsOn("wasmJsBrowserDistribution")
}

val pushTask = tasks.register<Exec>("push") {
    group = "webui"
    dependsOn("buildWebUI")
    commandLine("adb", "push", webDistDir.get().asFile.path, "/data/local/tmp/webroot")
}

val removeTask = tasks.register<Exec>("remove") {
    group = "webui"
    dependsOn(pushTask)
    commandLine("adb", "shell", "su", "-c", "rm -rf /data/adb/modules/$moduleId/webroot")
}

tasks.register<Exec>("install") {
    group = "webui"
    dependsOn(removeTask)
    commandLine("adb", "shell", "su", "-c", "mv /data/local/tmp/webroot /data/adb/modules/$moduleId/webroot")
}
