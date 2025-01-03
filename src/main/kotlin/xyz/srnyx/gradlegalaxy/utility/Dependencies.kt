package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.exclude

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.data.AdventureDependency
import xyz.srnyx.gradlegalaxy.enums.PaperVersion
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository


/**
 * 1. Sets the Java version for the project depending on the version
 * 2. Adds the [Repository.SONATYPE_SNAPSHOTS_OLD] repository if the version is 1.15 or below
 * 3. Adds the [Repository.MAVEN_CENTRAL] and [Repository.SPIGOT] repositories
 * 4. Adds the Spigot-API dependency (org.spigotmc:spigot-api:[getVersionString]) using the [configuration] and [configurationAction]
 *
 * @param version The version of Spigot-API to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added Spigot-API dependency
 */
@Ignore
fun Project.spigotAPI(
    version: String,
    configuration: String = "compileOnly",
    configurationAction: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    setJavaVersion(getJavaVersionForMC(version))
    val semanticVersion = SemanticVersion(version)
    if (semanticVersion.major <= 1 && semanticVersion.minor <= 15) repository(Repository.SONATYPE_SNAPSHOTS_OLD)
    repository(Repository.MAVEN_CENTRAL, Repository.SPIGOT)
    return addDependencyTo(dependencies, configuration, "org.spigotmc:spigot-api:${getVersionString(version)}", configurationAction)
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], maven local, and [Repository.SPIGOT] repositories
 * 2. Adds the Spigot dependency (org.spigotmc:spigot:[getVersionString]) using the [configuration] and [configurationAction]
 *
 * @param version The version of Spigot to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added Spigot dependency
 */
@Ignore
fun Project.spigotNMS(
    version: String,
    configuration: String = "compileOnly",
    configurationAction: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    setJavaVersion(getJavaVersionForMC(version))
    repository(Repository.MAVEN_CENTRAL, Repository.SPIGOT)
    repositories.mavenLocal()
    return addDependencyTo(dependencies, configuration, "org.spigotmc:spigot:${getVersionString(version)}", configurationAction)
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], [Repository.SONATYPE_SNAPSHOTS_OLD], and [Repository.PAPER] repositories
 * 2. Adds the Paper dependency ([PaperVersion.groupId]:[PaperVersion.artifactId]:[version]-R0.1-SNAPSHOT) using the [configuration] and [configurationAction]
 *
 * @param version The version of Paper to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added Paper dependency
 */
@Ignore
fun Project.paper(
    version: String,
    configuration: String = "compileOnly",
    configurationAction: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    setJavaVersion(getJavaVersionForMC(version))
    repository(Repository.MAVEN_CENTRAL, Repository.SONATYPE_SNAPSHOTS_OLD, Repository.PAPER)
    val paperVersion: PaperVersion = PaperVersion.parse(version)
    return addDependencyTo(dependencies, configuration, "${paperVersion.groupId}:${paperVersion.artifactId}:${getVersionString(version)}", configurationAction)
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL] repository
 * 2. Adds the dependencies to the provided Adventure components
 *
 * @param dependencies The Adventure dependencies to add
 * @param configurationAll The configuration to use for the dependencies if they don't have one specified
 */
@Ignore
fun Project.adventure(vararg dependencies: AdventureDependency, configurationAll: String? = null) {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    repository(Repository.MAVEN_CENTRAL)
    dependencies.forEach { addDependencyTo(project.dependencies, it.configuration ?: configurationAll ?: "implementation", "net.kyori:${it.component.getComponent()}:${it.version}", it.configurationAction) }
}

/**
 * 1. Adds the [Repository.JITPACK] and [Repository.ALESSIO_DP] (for Libby) repositories
 * 2. Adds the dependency to the provided Annoying API version
 *
 * @param version The version of Annoying API to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added Annoying API dependency
 */
fun Project.annoyingAPI(
    version: String,
    configuration: String = "implementation",
    configurationAction: ExternalModuleDependency.() -> Unit = {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    repository(Repository.JITPACK, Repository.ALESSIO_DP)

    // Runtime dependencies
    relocate("xyz.srnyx.annoyingapi")
    relocate("org.bstats")
    relocate("javassist.", getPackage() + ".libs.javassist.")
    relocate("org.reflections")
    relocate("de.tr7zw.changeme.nbtapi")

    return addDependencyTo(dependencies, configuration, "xyz.srnyx:annoying-api:$version") {
        exclude("net.byteflux", "libby-bukkit")
        exclude("xyz.srnyx", "java-utilities")
        configurationAction()
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL] repository
 * 2. Adds the dependency to the provided JDA version
 *
 * @param version The version of JDA to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added JDA dependency
 */
fun Project.jda(
    version: String,
    configuration: String = "implementation",
    configurationAction: ExternalModuleDependency.() -> Unit = {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    repository(Repository.MAVEN_CENTRAL)
    return addDependencyTo(dependencies, configuration, "net.dv8tion:JDA:$version", configurationAction)
}

/**
 * 1. Adds the [Repository.JITPACK] repository
 * 2. Adds the dependency to the provided Lazy Library version
 *
 * @param version The version of Lazy Library to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added Lazy Library dependency
 */
fun Project.lazyLibrary(
    version: String,
    configuration: String = "implementation",
    configurationAction: ExternalModuleDependency.() -> Unit = {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    repository(Repository.JITPACK)
    return addDependencyTo(dependencies, configuration, "xyz.srnyx:lazy-library:$version", configurationAction)
}

/**
 * 1. Adds the [Repository.JITPACK] repository
 * 2. Adds the dependency to the provided Magic Mongo version
 *
 * @param version The version of Magic Mongo to use
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 *
 * @return The [ExternalModuleDependency] of the added Magic Mongo dependency
 */
@Ignore
fun Project.magicMongo(
    version: String,
    configuration: String = "implementation",
    configurationAction: ExternalModuleDependency.() -> Unit = {}
): ExternalModuleDependency {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    repository(Repository.JITPACK)
    return addDependencyTo(dependencies, configuration, "xyz.srnyx:magic-mongo:$version", configurationAction)
}

/**
 * 1. Adds the provided dependency as an `implementation` dependency
 * 2. Relocates the dependency to the provided package
 *
 * @param project The project to relocate the dependency for
 * @param dependency The dependency to add
 * @param relocateFrom The package to relocate from
 * @param relocateTo The package to relocate to
 * @param configuration The configuration to add the dependency to
 *
 * @return The [T] of the added dependency
 */
@Ignore
fun <T: ModuleDependency> DependencyHandler.implementationRelocate(
    project: Project,
    dependency: T,
    relocateFrom: String,
    relocateTo: String = "${project.getPackage()}.libs.${relocateFrom.split(".").last()}",
    configuration: T.() -> Unit = {}
): T {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    project.relocate(relocateFrom, relocateTo)
    return add("implementation", dependency, configuration)
}

/**
 * 1. Adds the provided dependency as an `implementation` dependency
 * 2. Relocates the dependency to the provided package
 *
 * @param project The project to relocate the dependency for
 * @param dependency The dependency to add
 * @param relocateFrom The package to relocate from
 * @param relocateTo The package to relocate to
 * @param configuration The configuration to add the dependency to
 *
 * @return The [ExternalModuleDependency] of the added dependency
 */
@Ignore
fun DependencyHandler.implementationRelocate(
    project: Project,
    dependency: String,
    relocateFrom: String = dependency.split(":").first(),
    relocateTo: String = "${project.getPackage()}.libs.${relocateFrom.split(".").last()}",
    configuration: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    project.relocate(relocateFrom, relocateTo)
    return addDependencyTo(this, "implementation", dependency, configuration)
}

/**
 * Returns the correct Java version that is required for the Minecraft version
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
    val version = SemanticVersion(minecraftVersion)
    if (version.major > 1 || version.minor > 20 || (version.minor == 20 && version.patch >= 5)) return JavaVersion.VERSION_21
    if (version.minor >= 18) return JavaVersion.VERSION_17
    if (version.minor >= 17) return JavaVersion.VERSION_16
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
