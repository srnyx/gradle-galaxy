package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import xyz.srnyx.gradlegalaxy.utility.SemanticVersion
import xyz.srnyx.gradlegalaxy.utility.getJavaVersionForMC
import xyz.srnyx.gradlegalaxy.utility.getVersionString
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import javax.inject.Inject


abstract class SpigotApiExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(
    repositories = listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.SPIGOT, REPOSITORIES.SPIGOT_SNAPSHOTS),
    group = "org.spigotmc",
    name = "spigot-api",
    configurations = listOf("compileOnly", "testImplementation"),
) {
    @get:Input @get:Optional
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Runs in [xyz.srnyx.gradlegalaxy.extensions.Phase.WIRE] (before any [add]/[xyz.srnyx.gradlegalaxy.extensions.Phase.FINALIZE] across the whole
     * `galaxy { }` block) so this can never silently override a Java version another `galaxy { }` entry
     * decided on afterward — see [xyz.srnyx.gradlegalaxy.extensions.DeferredActions].
     */
    internal fun setup(project: Project) {
        if (setJavaVersion.get()) project.setJavaVersion(getJavaVersionForMC(version))

        val semanticVersion = SemanticVersion(version)
        if (semanticVersion.major <= 1 && semanticVersion.minor <= 15) project.repositories.maven(REPOSITORIES.SONATYPE_SNAPSHOTS_OLD)
    }

    override fun add(project: Project) {
        version = getVersionString(version)
        super.add(project)
    }
}

abstract class SpigotNmsExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(
    repositories = listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.SPIGOT, REPOSITORIES.SPIGOT_SNAPSHOTS),
    group = "org.spigotmc",
    name = "spigot",
    configurations = listOf("compileOnly", "testImplementation"),
) {
    @get:Input @get:Optional
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    internal fun setup(project: Project) {
        if (setJavaVersion.get()) project.setJavaVersion(getJavaVersionForMC(version))
        project.repositories.mavenLocal()
    }

    override fun add(project: Project) {
        version = getVersionString(version)
        super.add(project)
    }
}
