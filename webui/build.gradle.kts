plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.agp.app)
}

val moduleId: String by rootProject.extra
val moduleApplicationId: String by rootProject.extra
val androidCompileSdkVersion: Int by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra

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

    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
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

android {
    namespace = "io.github.nku100.webui"
    compileSdk = androidCompileSdkVersion
    defaultConfig {
        applicationId = moduleApplicationId
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = 1
        versionName = "1.0"
    }
}

// Generate ModuleInfo.kt with moduleId from module.gradle.kts
val generateModuleInfo = tasks.register("generateModuleInfo") {
    val outputDir = layout.buildDirectory.dir("generated/moduleInfo/commonMain/kotlin")
    val pkg = "io.github.nku100.webui"
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve(pkg.replace('.', '/'))
        dir.mkdirs()
        dir.resolve("ModuleInfo.kt").writeText(
            """
            |package $pkg
            |
            |/** Auto-generated from module.gradle.kts — do not edit. */
            |object ModuleInfo {
            |    const val MODULE_ID = "$moduleId"
            |    const val CONFIG_PATH = "/data/adb/modules/$moduleId/config.json"
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
