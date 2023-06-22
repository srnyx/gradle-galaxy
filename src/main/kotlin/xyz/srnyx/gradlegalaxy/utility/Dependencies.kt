package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.gradle.kotlin.dsl.add

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.enums.PaperVersion
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository


/**
 * 1. Sets the Java version for the project depending on the version
 * 2. Adds the [Repository.SONATYPE_SNAPSHOTS_OLD] repository if the version is 1.15 or below
 * 3. Adds the [Repository.MAVEN_CENTRAL] and [Repository.SPIGOT] repositories
 * 4. Returns the dependency annotation (org.spigotmc:spigot-api:[getVersionString])
 *
 * @param versionString The version of Spigot-API to use
 *
 * @return The dependency annotation for the provided Spigot-API version
 */
@Ignore
fun Project.spigotAPI(versionString: String): String {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    // Java version
    getJavaVersionForMC(versionString)?.let(::setJavaVersion)
    // Repositories
    val version = SemanticVersion(versionString)
    if (version.major <= 1 && version.minor <= 15) repository(Repository.SONATYPE_SNAPSHOTS_OLD)
    repository(Repository.MAVEN_CENTRAL, Repository.SPIGOT)
    // Dependency
    return "org.spigotmc:spigot-api:${getVersionString(versionString)}"
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], maven local, and [Repository.SPIGOT] repositories
 * 2. Returns the dependency annotation (org.spigotmc:spigot:[getVersionString])
 *
 * @param versionString The version of Spigot to use
 *
 * @return The dependency annotation for the provided Spigot version
 */
@Ignore
fun Project.spigotNMS(versionString: String): String {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    // Java version
    getJavaVersionForMC(versionString)?.let(::setJavaVersion)
    // Repositories
    repository(Repository.MAVEN_CENTRAL, Repository.SPIGOT)
    repositories.mavenLocal()
    // Dependency
    return "org.spigotmc:spigot:${getVersionString(versionString)}"
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], [Repository.SONATYPE_SNAPSHOTS_OLD], and [Repository.PAPER] repositories
 * 2. Returns the dependency annotation ([PaperVersion.groupId]:[PaperVersion.artifactId]:[version]-R0.1-SNAPSHOT)
 *
 * @param version The version of Paper to use
 *
 * @return The dependency annotation for the provided Paper version
 */
@Ignore
fun Project.paper(version: String): String {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    // Java version
    getJavaVersionForMC(version)?.let(::setJavaVersion)
    // Repositories
    repository(Repository.MAVEN_CENTRAL, Repository.SONATYPE_SNAPSHOTS_OLD, Repository.PAPER)
    // Dependency
    val paperVersion: PaperVersion = PaperVersion.parse(version)
    return "${paperVersion.groupId}:${paperVersion.artifactId}:${getVersionString(version)}"
}

/**
 * 1. Adds the [Repository.JITPACK] repository
 * 2. Returns the dependency to the provided Annoying API version
 *
 * @param version The version of Annoying API to use
 *
 * @return The dependency annotation for the provided Annoying API version
 */
fun Project.annoyingAPI(version: String): String {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    // Repositories
    repository(Repository.JITPACK)
    // Dependency
    return "xyz.srnyx:annoying-api:$version"
}

/**
 * 1. Adds the provided dependency as an `implementation` dependency
 * 2. Relocates the dependency to the provided package
 */
@Ignore
fun <T: ModuleDependency> DependencyHandler.implementationRelocate(
    project: Project,
    dependency: T,
    relocateFrom: String,
    relocateTo: String = "${project.group}.${project.name.lowercase().filter { char -> char.isLetterOrDigit() || char in "._" }}.libs.${relocateFrom.split(".").last()}",
    configuration: T.() -> Unit = {}
): T {
    check(project.hasShadowPlugin()) { "Shadow plugin is not applied!" }
    project.relocate(relocateFrom, relocateTo)
    return add("implementation", dependency, configuration)
}

/**
 * 1. Adds the provided dependency as an `implementation` dependency
 * 2. Relocates the dependency to the provided package
 */
@Ignore
fun DependencyHandler.implementationRelocate(
    project: Project,
    dependency: String,
    relocateFrom: String = dependency.split(":").first(),
    relocateTo: String = "${project.group}.${project.name.lowercase().filter { char -> char.isLetterOrDigit() || char in "._" }}.libs.${relocateFrom.split(".").last()}",
    configuration: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency {
    check(project.hasShadowPlugin()) { "Shadow plugin is not applied!" }
    project.relocate(relocateFrom, relocateTo)
    return addDependencyTo(this, "implementation", dependency, configuration)
}

/**
 * Returns the correct Java version that is required for the Minecraft version
 */
fun getJavaVersionForMC(minecraftVersion: String): JavaVersion? {
    val version = SemanticVersion(minecraftVersion)
    if (version.major != 1 || version.minor >= 18) return null
    return JavaVersion.VERSION_1_8
}

/**
 * Returns the version string with `-R0.1-SNAPSHOT` appended to it
 */
fun getVersionString(version: String): String = "${version}-R0.1-SNAPSHOT"
