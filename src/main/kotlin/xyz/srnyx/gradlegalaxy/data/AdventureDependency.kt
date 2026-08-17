package xyz.srnyx.gradlegalaxy.data

import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.enums.AdventureComponent
import xyz.srnyx.gradlegalaxy.enums.Component
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES

/**
 * Represents a dependency on a component of Adventure
 *
 * @param component The component to depend on
 * @param config The configuration for the dependency
 */
data class AdventureDependency(val component: Component, val config: DependencyConfig) {
    internal fun toExtension(objects: ObjectFactory): DependencyExtension {
        if (config.configurations == null) {
            if (component == AdventureComponent.BOM) {
                config.configurations = listOf("testImplementation")
            } else if (component == AdventureComponent.ANNOTATION_PROCESSORS) {
                config.configurations = listOf("annotationProcessor")
            }
        }

        return DependencyExtension(objects).apply {
            repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL))
            group.set("net.kyori")
            name.set(component.getComponent())
            configurations.set(config.configurations ?: listOf("implementation"))
            platform.set(component == AdventureComponent.BOM)
            version.set(config.version)
            action = config.configurationAction
        }
    }
}
