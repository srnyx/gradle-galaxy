package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.artifacts.ExternalModuleDependency


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
)
