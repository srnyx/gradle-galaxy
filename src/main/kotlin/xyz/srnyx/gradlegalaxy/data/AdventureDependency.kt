package xyz.srnyx.gradlegalaxy.data

import org.gradle.api.artifacts.ExternalModuleDependency

import xyz.srnyx.gradlegalaxy.enums.Component


/**
 * Represents a dependency on a component of Adventure
 *
 * @param component The component to depend on
 * @param version The version of the component to depend on
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 */
data class AdventureDependency(val component: Component, val version: String, val configuration: String? = null, val configurationAction: ExternalModuleDependency.() -> Unit = {})
