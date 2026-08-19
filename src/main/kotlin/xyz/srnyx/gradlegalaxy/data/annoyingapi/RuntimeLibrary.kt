package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.extensions.minecraft.RuntimeLibrariesExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.RuntimeLibraryExtension


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
) {
    internal fun toExtension(
        objects: ObjectFactory,
        runtimeLibraries: RuntimeLibrariesExtension,
    ): RuntimeLibraryExtension = objects.newInstance(RuntimeLibraryExtension::class.java, runtimeLibraries, name).apply {
        val data = this@RuntimeLibrary

        repositories.set(data.repositories)
        group.set(data.group)
        artifact.set(data.artifact)
        version.set(data.version)
        dependencies.set(data.dependencies)

        // Relocations
        data.relocations.forEach { relocate(it.toExtension()) }

        // Add excludes through action
        action = { data.excludes.forEach { exclude(it.group, it.module) } }
    }
}
