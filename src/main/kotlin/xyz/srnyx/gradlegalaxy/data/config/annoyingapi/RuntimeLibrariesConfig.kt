package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import xyz.srnyx.gradlegalaxy.extensions.minecraft.RuntimeLibrariesExtension


/** Configuration for a set of runtime library dependencies */
data class RuntimeLibrariesConfig(
    @Deprecated("Doesn't do anything anymore") val addRepositories: Boolean = true,
    /**
     * Dependency classpaths to add the dependencies to (e.g. `compileOnly`, `implementation`, `testImplementation`, etc.).
     *
     * If empty, dependencies will not be added to any classpath.
     *
     * You usually don't need to change this as dependencies are on the `compileOnlyApi` classpath on Annoying API.
     */
    val configurations: List<String> = listOf("testImplementation"),
    val relocate: Boolean = true,
) {
    internal fun toExtension(): RuntimeLibrariesExtension.() -> Unit = {
        val config = this@RuntimeLibrariesConfig

        configurations.set(config.configurations)
        relocate.set(config.relocate)
    }
}
