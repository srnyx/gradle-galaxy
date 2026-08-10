package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.configuration.problems.logger
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.data.config.DependencyExtension
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupExtension
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.getAnnoyingApiMetadata
import xyz.srnyx.gradlegalaxy.utility.relocate
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import javax.inject.Inject


open class MetadataConfig(
    var useMetadata: Boolean = true,
    var relocateAnnoyingAPI: Boolean = true,
    var setJavaVersion: Boolean = true,
    var addRepositories: Boolean = true,
    var excludes: Boolean = true,
    var runtimeLibrariesConfig: RuntimeLibrariesConfig = RuntimeLibrariesConfig(),
) {
    internal fun toExtension(
        libraries: List<RuntimeLibrary>
    ): MetadataExtension.() -> Unit = {
        val config = this@MetadataConfig

        useMetadata = config.useMetadata
        relocateAnnoyingAPI = config.relocateAnnoyingAPI
        setJavaVersion = config.setJavaVersion
        addRepositories = config.addRepositories
        excludes = config.excludes
        runtimeLibraries(libraries, runtimeLibrariesConfig.toExtension())
    }
}

abstract class MetadataExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var useMetadata: Boolean = true
    var relocateAnnoyingAPI: Boolean = true
    var setJavaVersion: Boolean = true
    var addRepositories: Boolean = true
    var excludes: Boolean = true

    var runtimeLibraries = objects.newInstance(RuntimeLibrariesExtension::class.java)

    fun runtimeLibraries(libraries: List<RuntimeLibrary>, action: RuntimeLibrariesExtension.() -> Unit) {
        runtimeLibraries.libraries = libraries
        runtimeLibraries.action()
    }

    internal fun process(
        project: Project,
        annoyingApiDependency: DependencyExtension,
        java: JavaSetupExtension,
    ): AnnoyingMetadata? {
        // Get and process Annoying API metadata
        val metadata = useMetadata.takeIf { it }?.let { project.getAnnoyingApiMetadata(annoyingApiDependency.version) }
        logger.error("metadata: $metadata")
        if (metadata != null) {
            // Relocate Annoying API
            if (relocateAnnoyingAPI) project.relocate(metadata.packageName)

            // Java version (only if custom not specified)
            logger.error("setJavaVersion: $setJavaVersion metadata.javaVersion: ${metadata.javaVersion} java.javaVersion: ${java.javaVersion}")
            if (setJavaVersion && metadata.javaVersion != null && java.javaVersion == null) {
                project.setJavaVersion(JavaVersion.toVersion(metadata.javaVersion))
            }

            // Repositories
            if (addRepositories) metadata.repositories.forEach { project.repository(it) }

            // Runtime libraries
            runtimeLibraries.libraries += metadata.runtimeLibraries
            runtimeLibraries.process(project)
        }

        // Excludes
        if (excludes) {
            val original = annoyingApiDependency.action
            annoyingApiDependency.action = {
                metadata?.excludes?.forEach { exclude(it.group, it.module) }
                original()
            }
        }

        return metadata
    }
}

