package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.enums.PaperVersion
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.mavenQuick


/**
 * 1. Sets the Java version for the project depending on the version
 * 2. Adds the [Repository.SONATYPE_SNAPSHOTS_OLD] repository if the version is 1.15 or below
 * 3. Adds the [Repository.MAVEN_CENTRAL] and [Repository.SPIGOT] repositories
 * 4. Adds the dependency (org.spigotmc:spigot-api:[getVersionString])
 */
fun DependencyHandler.spigotAPI(project: Project? = null, versionString: String): String {
    if (project != null) {
        // Java version
        getJavaVersionForMC(versionString)?.let { project.setJavaVersion(it) }
        // Repositories
        val version = SemanticVersion(versionString)
        if (version.major <= 1 && version.minor <= 15) project.repositories.mavenQuick(Repository.SONATYPE_SNAPSHOTS_OLD)
        project.repositories.mavenQuick(Repository.MAVEN_CENTRAL, Repository.SPIGOT)
    }
    // Dependency
    return "org.spigotmc:spigot-api:${getVersionString(versionString)}"
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], maven local, and [Repository.SPIGOT] repositories
 * 2. Adds the dependency (org.spigotmc:spigot:[getVersionString])
 */
@Ignore
fun DependencyHandler.spigotNMS(project: Project? = null, versionString: String): String {
    if (project != null) {
        // Java version
        getJavaVersionForMC(versionString)?.let { project.setJavaVersion(it) }
        // Repositories
        project.repositories.mavenQuick(Repository.MAVEN_CENTRAL, Repository.SPIGOT)
        project.repositories.mavenLocal()
    }
    // Dependency
    return "org.spigotmc:spigot:${getVersionString(versionString)}"
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], [Repository.SONATYPE_SNAPSHOTS_OLD], and [Repository.PAPER] repositories
 * 2. Adds the dependency ([PaperVersion.groupId]:[PaperVersion.artifactId]:[version]-R0.1-SNAPSHOT)
 */
@Ignore
fun DependencyHandler.paper(version: String, project: Project? = null): String {
    if (project != null) {
        // Java version
        getJavaVersionForMC(version)?.let { project.setJavaVersion(it) }
        // Repositories
        project.repositories.mavenQuick(Repository.MAVEN_CENTRAL, Repository.SONATYPE_SNAPSHOTS_OLD, Repository.PAPER)
    }
    // Dependency
    val paperVersion: PaperVersion = PaperVersion.parse(version)
    return "${paperVersion.groupId}:${paperVersion.artifactId}:${getVersionString(version)}"
}

/**
 * Returns the version string with `-R0.1-SNAPSHOT` appended to it
 */
fun getVersionString(version: String): String = "${version}-R0.1-SNAPSHOT"

/**
 * Returns the correct Java version that is required for the Minecraft version
 */
fun getJavaVersionForMC(minecraftVersion: String): JavaVersion? {
    val version = SemanticVersion(minecraftVersion)
    if (version.major != 1 || version.minor >= 18) return null
    return JavaVersion.VERSION_1_8
}