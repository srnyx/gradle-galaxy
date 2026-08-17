package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.artifacts.ModuleDependency
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension


/**
 * Configuration for a dependency, including version, configurations, and configuration action
 *
 * @param version The version of the dependency
 * @param configuration The configuration to add the dependency to. Automatically populates [configurations] if set (default: `null`)
 * @param configurations The configurations to add the dependency to. Defaults to a list containing [configuration] if provided, otherwise `null` (default: `null`)
 * @param configurationAction The action to apply to the dependency (default: `{}`)
 *
 * @deprecated Use `galaxy { }`'s dependency functions (e.g. `magicMongo(version) { }`) instead
 */
data class DependencyConfig(
    val version: String,
    @Deprecated("Use configurations instead") val configuration: String? = null,
    var configurations: List<String>? = configuration?.let { listOf(it) },
    var configurationAction: ModuleDependency.() -> Unit = {}
) {
    internal fun toExtension(): DependencyExtension.() -> Unit = {
        val config: DependencyConfig = this@DependencyConfig

        config.configurations
            ?.takeIf { it.isNotEmpty() }
            ?.let { configurations.set(it) }
        action = config.configurationAction
    }
}
