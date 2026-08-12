package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import xyz.srnyx.gradlegalaxy.extensions.MetadataExtension


open class MetadataConfig(
    var useMetadata: Boolean = true,
    var relocateAnnoyingAPI: Boolean = true,
    var setJavaVersion: Boolean = true,
    var addRepositories: Boolean = true,
    var excludes: Boolean = true,
    var runtimeLibrariesConfig: RuntimeLibrariesConfig = RuntimeLibrariesConfig(),
) {
    internal fun toExtension(): MetadataExtension.() -> Unit = {
        val config = this@MetadataConfig

        useMetadata.set(config.useMetadata)
        relocateAnnoyingAPI.set(config.relocateAnnoyingAPI)
        setJavaVersion.set(config.setJavaVersion)
        addRepositories.set(config.addRepositories)
        excludes.set(config.excludes)
        runtimeLibraries(runtimeLibrariesConfig.toExtension())
    }
}
