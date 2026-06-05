package xyz.srnyx.gradlegalaxy.data.config.annoyingapi


data class MetadataConfig(
    val useMetadata: Boolean = true,
    val relocateAnnoyingAPI: Boolean = true,
    val setJavaVersion: Boolean = true,
    val addRepositories: Boolean = true,
    val excludes: Boolean = true,
    val runtimeLibrariesConfig: RuntimeLibrariesConfig = RuntimeLibrariesConfig(),
)
