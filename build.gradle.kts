import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

apply(from = "module.gradle.kts")

// Native and preview log tag. Falls back to moduleId when moduleName has no ASCII letters or digits.
val moduleLogTag by extra(
    (extra["moduleName"] as String).filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' }
        .ifEmpty { extra["moduleId"] as String }
)

val moduleRepo by extra(
    providers.exec {
        commandLine("git", "remote", "get-url", "origin")
        // A checkout without origin is treated as an unknown repository instead of failing the build.
        isIgnoreExitValue = true
    }.standardOutput.asText.map { url ->
            val trimmed = url.trim()
            val match = Regex("""(?:https://github\.com/|git@github\.com:)([^/]+)/([^/]+?)(?:\.git)?$""").find(trimmed)
            if (match != null) "https://github.com/${match.groupValues[1]}/${match.groupValues[2]}" else trimmed
        }.getOrElse("")
)

val androidMinSdkVersion by extra(26)
val androidTargetSdkVersion by extra(36)
val androidCompileSdkVersion by extra(37)
val androidBuildToolsVersion by extra("36.0.0")
val androidCompileNdkVersion by extra("29.0.14206865")
val androidSourceCompatibility by extra(JavaVersion.VERSION_17)
val androidTargetCompatibility by extra(JavaVersion.VERSION_17)

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

fun Project.configureBaseExtension() {
    extensions.findByType(ApplicationExtension::class)?.run {
        namespace = rootProject.extra["moduleApplicationId"] as String
        compileSdk = androidCompileSdkVersion
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
        }

        compileOptions {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
        }
    }

}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withType(JavaPlugin::class.java) {
        extensions.configure(JavaPluginExtension::class.java) {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
        }
    }
}
