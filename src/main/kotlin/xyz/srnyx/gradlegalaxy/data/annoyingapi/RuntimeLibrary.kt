package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable


@Serializable
data class RuntimeLibrary(
    val name: String,
    val repositories: List<String> = emptyList(),
    val group: String,
    val artifact: String,
    val version: String,
    val excludes: List<Exclude> = emptyList(),
    val relocations: List<Relocation> = emptyList(),
    /**
     * Names of other RuntimeLibraries that this library depends on
     */
    val dependencies: List<String> = emptyList(),
)
