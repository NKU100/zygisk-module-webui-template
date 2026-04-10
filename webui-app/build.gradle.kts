plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val moduleApplicationId = rootProject.extra["moduleApplicationId"] as String
val androidCompileSdkVersion = rootProject.extra["androidCompileSdkVersion"] as Int
val androidMinSdkVersion = rootProject.extra["androidMinSdkVersion"] as Int
val androidTargetSdkVersion = rootProject.extra["androidTargetSdkVersion"] as Int

android {
    namespace = "io.github.nku100.webui.app"
    compileSdk = androidCompileSdkVersion
    defaultConfig {
        applicationId = moduleApplicationId
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":webui"))
    implementation(libs.activity.compose)
}
