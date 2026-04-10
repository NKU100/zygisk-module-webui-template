package io.github.nku100.webui.data

import io.github.nku100.webui.ModuleInfo
import io.github.nku100.webui.platform.PlatformBridge
import kotlinx.serialization.json.Json

/**
 * Repository that manages reading/writing module configuration.
 * Config is stored as JSON at the module's data directory.
 * Path is auto-generated from moduleId in module.gradle.kts.
 */
object ConfigRepository {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    var configPath: String = ModuleInfo.CONFIG_PATH

    suspend fun load(): ModuleConfig {
        return try {
            val content = PlatformBridge.readFile(configPath)
            if (content.isBlank()) ModuleConfig() else json.decodeFromString(content)
        } catch (_: Exception) {
            ModuleConfig()
        }
    }

    suspend fun save(config: ModuleConfig) {
        val content = json.encodeToString(ModuleConfig.serializer(), config)
        PlatformBridge.writeFile(configPath, content)
    }
}
