package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.maven
import org.gradle.util.internal.VersionNumber
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import javax.inject.Inject


abstract class SpigotApiExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.SPIGOT, REPOSITORIES.SPIGOT_SNAPSHOTS))
        group.set("org.spigotmc")
        name.set("spigot-api")
        configurations.set(listOf("compileOnly", "testImplementation"))
    } }
    @get:Input @get:Optional
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Runs in [xyz.srnyx.gradlegalaxy.extensions.Phase.WIRE] (before any [add]/[xyz.srnyx.gradlegalaxy.extensions.Phase.FINALIZE] across the whole
     * `galaxy { }` block) so this can never silently override a Java version another `galaxy { }` entry
     * decided on afterward — see [xyz.srnyx.gradlegalaxy.extensions.DeferredActions].
     */
    internal fun setup(project: Project) {
        if (setJavaVersion.get()) project.setJavaVersion(getJavaVersionForMC(version.get()), false)

        val semanticVersion = VersionNumber.parse(version.get())
        if (semanticVersion.major <= 1 && semanticVersion.minor <= 15) project.repositories.maven(REPOSITORIES.SONATYPE_SNAPSHOTS_OLD)
    }

    override fun add(project: Project) {
        version.set(getVersionString(version.get()))
        super.add(project)
    }
}

abstract class SpigotNmsExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.SPIGOT, REPOSITORIES.SPIGOT_SNAPSHOTS))
        group.set("org.spigotmc")
        name.set("spigot")
        configurations.set(listOf("compileOnly", "testImplementation"))
    } }
    @get:Input @get:Optional
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    internal fun setup(project: Project) {
        if (setJavaVersion.get()) project.setJavaVersion(getJavaVersionForMC(version.get()), false)
        project.repositories.mavenLocal()
    }

    override fun add(project: Project) {
        version.set(getVersionString(version.get()))
        super.add(project)
    }
}

/**
 * Returns the correct Java version that is required for the Minecraft version
 * - 26.1+: Java 25
 * - 1.20.5+: Java 21
 * - 1.18+: Java 17
 * - 1.17+: Java 16
 * - Else: Java 8
 *
 * @param minecraftVersion The Minecraft version to get the Java version for
 *
 * @return The [JavaVersion] that is required for the Minecraft version
 */
fun getJavaVersionForMC(minecraftVersion: String): JavaVersion {
    val version = VersionNumber.parse(minecraftVersion)
    // 26.1+
    if (version.major > 1) return JavaVersion.VERSION_25
    // 1.20.5+
    if (version.minor > 20 || (version.minor == 20 && version.patch >= 5)) return JavaVersion.VERSION_21
    // 1.18+
    if (version.minor >= 18) return JavaVersion.VERSION_17
    // 1.17+
    if (version.minor >= 17) return JavaVersion.VERSION_16
    // Else
    return JavaVersion.VERSION_1_8
}

/**
 * Returns the version string with `-R0.1-SNAPSHOT` appended to it
 *
 * @param version The version to append to
 *
 * @return The version string with `-R0.1-SNAPSHOT` appended to it
 */
fun getVersionString(version: String): String = "${version}-R0.1-SNAPSHOT"
