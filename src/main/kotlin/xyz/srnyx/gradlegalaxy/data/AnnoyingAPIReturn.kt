package xyz.srnyx.gradlegalaxy.data

import org.gradle.api.artifacts.ExternalModuleDependency

import xyz.srnyx.gradlegalaxy.utility.setupAnnoyingAPI


class AnnoyingAPIReturn(private val dependencyModule: ExternalModuleDependency? = null, private val annoyingAPIModule: ExternalModuleDependency) {
    /**
     * The configuration for the dependency that was added via [setupAnnoyingAPI]
     */
    fun dependency(configuration: ExternalModuleDependency.() -> Unit) = dependencyModule?.let(configuration)

    /**
     * The configuration for Annoying API's implementation that was added via [setupAnnoyingAPI]
     */
    fun annoyingAPI(configuration: ExternalModuleDependency.() -> Unit) = annoyingAPIModule.configuration()
}
