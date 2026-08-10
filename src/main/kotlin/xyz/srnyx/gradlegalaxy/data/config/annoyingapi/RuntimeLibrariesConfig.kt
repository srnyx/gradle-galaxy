package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import org.gradle.api.Project
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.getPackage
import xyz.srnyx.gradlegalaxy.utility.relocate
import javax.inject.Inject
import kotlin.text.replace


data class RuntimeLibrariesConfig(
    val addRepositories: Boolean = true,
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
        addRepositories = this@RuntimeLibrariesConfig.addRepositories
        configurations = this@RuntimeLibrariesConfig.configurations
        relocate = this@RuntimeLibrariesConfig.relocate
    }
}

abstract class RuntimeLibrariesExtension @Inject constructor() {
    lateinit var libraries: List<RuntimeLibrary>
    var addRepositories: Boolean = true
    /**
     * Dependency classpaths to add the dependencies to (e.g. `compileOnly`, `implementation`, `testImplementation`, etc.).
     *
     * If empty, dependencies will not be added to any classpath.
     *
     * You usually don't need to change this as dependencies are on the `compileOnlyApi` classpath on Annoying API.
     */
    var configurations: List<String> = listOf("testImplementation")
    var relocate: Boolean = true

    internal fun process(project: Project) {
        if (!::libraries.isInitialized || libraries.isEmpty()) return

        val getPackage = project.getPackage()
        libraries.forEach { library ->
            // Add repositories
            if (addRepositories) library.repositories.forEach { repo -> project.repository(repo) }

            // Add dependencies
            configurations.forEach { configuration ->
                project.dependencies.add(configuration, "${library.group}:${library.artifact}:${library.version}") {
                    // Excludes
                    library.excludes.forEach { exclude(it.group, it.module) }
                }
            }

            // Relocations
            if (relocate) library.relocations.forEach { relocation ->
                val to = relocation.to?.replace("{package}", getPackage)
                if (to != null) {
                    project.relocate(relocation.from, to)
                } else {
                    project.relocate(relocation.from)
                }
            }
        }
    }
}
