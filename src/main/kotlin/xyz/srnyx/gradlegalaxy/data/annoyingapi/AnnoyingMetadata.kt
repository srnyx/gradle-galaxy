package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable


@Serializable
data class AnnoyingMetadata(
    val packageName: String,
    val javaVersion: Int? = null,
    val repositories: List<String> = emptyList(),
    val runtimeLibraries: List<RuntimeLibrary> = emptyList(),
    val excludes: List<Exclude> = emptyList(),
) {
    fun getRuntimeLibrary(name: String): RuntimeLibrary? = runtimeLibraries.find { it.name == name }

}
