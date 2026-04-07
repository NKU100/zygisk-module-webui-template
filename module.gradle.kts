val moduleId by extra("zygisk_sample")
val moduleName by extra("Zygisk Module Sample")
val moduleVersion by extra("v1.0.0")
val moduleVersionCode by extra(1)
val moduleAuthor by extra("NKU100")
val moduleRepo by extra(
    try {
        val remoteUrl = providers.exec { commandLine("git", "remote", "get-url", "origin") }
            .standardOutput.asText.map { it.trim() }.get()
        val match = Regex("""(?:https://github\.com/|git@github\.com:)([^/]+)/([^/]+?)(?:\.git)?$""").find(remoteUrl)
        if (match != null) "https://github.com/${match.groupValues[1]}/${match.groupValues[2]}" else remoteUrl
    } catch (_: Exception) {
        ""
    }
)
val moduleDesc by extra("A sample module for zygisk")
val moduleApplicationId by extra("io.github.nku100.zygisk.sample")
val moduleLibName by extra("sample")
val abiList by extra(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))