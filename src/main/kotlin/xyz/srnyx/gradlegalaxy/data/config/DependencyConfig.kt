package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.artifacts.ExternalModuleDependency


/**
 * Configuration for a dependency, including version, configuration, and configuration action
 *
 * @param version The version of the dependency
 * @param configuration The configuration to add the dependency to (default: `null`, which uses the default configuration for the function)
 * @param configurationAction The action to apply to the dependency (default: `{}`)
 */
data class DependencyConfig(
    val version: String,
    val configuration: String? = null,
    var configurationAction: ExternalModuleDependency.() -> Unit = {}
)
