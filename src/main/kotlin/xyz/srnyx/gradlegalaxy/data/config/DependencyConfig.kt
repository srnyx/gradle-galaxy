package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.internal.configuration.problems.logger
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.hasJavaPlugin
import javax.inject.Inject


/**
 * Configuration for a dependency, including version, configurations, and configuration action
 *
 * @param version The version of the dependency
 * @param configuration The configuration to add the dependency to. Automatically populates [configurations] if set (default: `null`)
 * @param configurations The configurations to add the dependency to. Defaults to a list containing [configuration] if provided, otherwise `null` (default: `null`)
 * @param configurationAction The action to apply to the dependency (default: `{}`)
 *
 * @deprecated Use [configurations] instead of [configuration]
 */
data class DependencyConfig(
    val version: String,
    @Deprecated("Use configurations instead") val configuration: String? = null,
    val configurations: List<String>? = configuration?.let { listOf(it) },
    var configurationAction: ExternalModuleDependency.() -> Unit = {}
) {
    internal fun toExtension(): DependencyExtension.() -> Unit = {
        val config: DependencyConfig = this@DependencyConfig

        configurations = config.configurations ?: configuration?.let { listOf(it) } ?: emptyList()
        action = config.configurationAction
    }
}

open class DependencyExtension @Inject constructor(
    var repositories: List<String>,
    var group: String,
    var name: String,
    var configurations: List<String>,
) {
    var configured: Boolean = false

    lateinit var version: String
    var action: ExternalModuleDependency.() -> Unit = {}

    fun action(action: ExternalModuleDependency.() -> Unit) {
        val previousAction = this.action
        this.action = {
            previousAction(this)
            action(this)
        }
    }

    internal open fun add(project: Project) {
        check(::version.isInitialized) { "Dependency version is not configured!" }
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }

        // Repositories
        repositories.forEach { project.repository(it) }

        // Add dependency
        configurations.forEach { configuration ->
            addDependencyTo<ExternalModuleDependency>(project.dependencies, configuration, "${group}:${name}:${version}") {
                action(this)
            }
        }
    }
}
