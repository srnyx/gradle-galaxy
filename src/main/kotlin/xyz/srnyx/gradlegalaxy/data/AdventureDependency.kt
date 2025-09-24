package xyz.srnyx.gradlegalaxy.data

import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.enums.Component


/**
 * Represents a dependency on a component of Adventure
 *
 * @param component The component to depend on
 * @param config The configuration for the dependency
 */
data class AdventureDependency(val component: Component, val config: DependencyConfig)