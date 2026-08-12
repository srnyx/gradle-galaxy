package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.AdventureDependency
import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.dependency.MockBukkitConfig
import xyz.srnyx.gradlegalaxy.data.config.dependency.SpigotConfig
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.extensions.GradleGalaxyExtension


/**
 * 1. Sets the Java version for the project depending on the version
 * 2. Adds the [Repository.SONATYPE_SNAPSHOTS_OLD] repository if the version is 1.15 or below
 * 3. Adds the [Repository.MAVEN_CENTRAL] and [Repository.SPIGOT] repositories
 * 4. Adds the Spigot-API dependency (org.spigotmc:spigot-api:[getVersionString])
 *
 * @param config The configuration for the Spigot-API dependency
 */
@Deprecated("Use galaxy { spigotAPI(version) { ... } } instead")
fun Project.spigotAPI(
    config: DependencyConfig,
    spigotConfig: SpigotConfig = SpigotConfig(),
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        minecraft {
            spigotAPI(config.version) {
                config.toExtension()(this)
                setJavaVersion.set(spigotConfig.setJavaVersion)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], maven local, and [Repository.SPIGOT] repositories
 * 2. Adds the Spigot dependency (org.spigotmc:spigot:[getVersionString])
 *
 * @param config The configuration for the Spigot dependency
 */
@Deprecated("Use galaxy { spigotNMS(version) { ... } } instead")
fun Project.spigotNMS(
    config: DependencyConfig,
    spigotConfig: SpigotConfig = SpigotConfig(),
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        minecraft {
            spigotNMS(config.version) {
                config.toExtension()(this)
                setJavaVersion.set(spigotConfig.setJavaVersion)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], [Repository.SONATYPE_SNAPSHOTS_OLD], and [Repository.PAPER] repositories
 * 2. Adds the Paper dependency (`group`:`artifact`:`version`-R0.1-SNAPSHOT)
 *
 * @param config The configuration for the Paper dependency
 */
@Deprecated("Use galaxy { paper(version) { ... } } instead")
fun Project.paper(
    config: DependencyConfig,
    spigotConfig: SpigotConfig = SpigotConfig(),
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        minecraft {
            paper(config.version) {
                config.toExtension()(this)
                setJavaVersion.set(spigotConfig.setJavaVersion)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL] repository
 * 2. Adds the dependencies to the provided Adventure components
 *
 * @param dependencies The Adventure dependencies to add
 * @param configurationAll The configuration to use for the dependencies if they don't have one specified
 */
@Used
fun Project.adventure(vararg dependencies: AdventureDependency, configurationAll: String? = null) {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Repositories
    repository(Repository.MAVEN_CENTRAL)

    // Add dependencies
    dependencies.forEach { dependency ->
        val configurations = dependency.config.configurations ?: listOf(configurationAll ?: "implementation")
        configurations.forEach { configuration ->
            addDependencyTo(project.dependencies, configuration, "net.kyori:${dependency.component.getComponent()}:${dependency.config.version}", dependency.config.configurationAction)
        }
    }
}

/**
 * 1. Adds srnyx's repositories and [Repository.ALESSIO_DP] (for Libby) repositories
 * 2. Relocates `xyz.srnyx.annoyingapi`
 * 3. Adds the dependency to the provided Annoying API version
 *
 * @param config The configuration for the Annoying API dependency
 */
@Deprecated("Use galaxy { annoyingAPI(version) { ... } } instead")
fun Project.annoyingAPI(
    config: DependencyConfig,
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        minecraft {
            annoyingAPI(config.version) {
                config.toExtension()(this)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL] repository
 * 2. Adds the dependency to the provided JDA version
 *
 * @param config The configuration for the JDA dependency
 */
@Deprecated("Use galaxy { jda(version) { ... } } instead")
fun Project.jda(
    config: DependencyConfig,
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        discord {
            jda(config.version) {
                config.toExtension()(this)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds srnyx's repository
 * 2. Adds the dependency to the provided Lazy Library version
 *
 * @param config The configuration for the Lazy Library dependency
 */
@Deprecated("Use galaxy { lazyLibrary(version) { ... } } instead")
fun Project.lazyLibrary(
    config: DependencyConfig,
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        discord {
            lazyLibrary(config.version) {
                config.toExtension()(this)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds srnyx's repository
 * 2. Adds the dependency to the provided Magic Mongo version
 *
 * @param config The configuration for the Magic Mongo dependency
 */
@Deprecated("Use galaxy { magicMongo(version) { ... } } instead")
fun Project.magicMongo(
    config: DependencyConfig,
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        magicMongo(config.version) {
            config.toExtension()(this)
            action(block)
        }
    }
}

/**
 * 1. Adds [Repository.MAVEN_CENTRAL] and [Repository.PAPER] repositories
 * 2. Adds the dependency to the provided MockBukkit version
 *
 * @param config The configuration for the MockBukkit dependency
 * @param mockBukkitConfig The configuration for MockBukkit
 * @param block The block to apply to the dependency
 */
@Deprecated("Use galaxy { mockBukkit(version) { ... } } instead")
fun Project.mockBukkit(
    config: DependencyConfig,
    mockBukkitConfig: MockBukkitConfig = MockBukkitConfig(),
    block: ModuleDependency.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        testing {
            mockBukkit(config.version) {
                config.toExtension()(this)
                mockBukkitConfig.toExtension()(this)
                action(block)
            }
        }
    }
}

/**
 * 1. Adds the provided dependency as an `implementation` dependency
 * 2. Relocates the dependency to the provided package
 *
 * @param dependency The dependency to add
 * @param relocateFrom The package to relocate from
 * @param relocateTo The package to relocate to
 * @param configurationAction The configuration action for the dependency
 *
 * @return The [T] of the added dependency
 */
@Used
fun <T: ModuleDependency> Project.dependencyRelocate(
    dependency: T,
    relocateFrom: String,
    relocateTo: String = "${project.getPackage()}.libs.${relocateFrom.split(".").last()}",
    configuration: String = "implementation",
    configurationAction: T.() -> Unit = {}
): T {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    project.relocate(relocateFrom, relocateTo)
    return addDependencyTo(dependencies, configuration, dependency, configurationAction)
}

/**
 * 1. Adds the provided dependency as an `implementation` dependency
 * 2. Relocates the dependency to the provided package
 *
 * @param dependency The dependency to add
 * @param relocateFrom The package to relocate from
 * @param relocateTo The package to relocate to
 * @param configurationAction The configuration to add the dependency to
 *
 * @return The [ExternalModuleDependency] of the added dependency
 */
@Used
fun Project.dependencyRelocate(
    dependency: String,
    relocateFrom: String = dependency.split(":").first(),
    relocateTo: String = "${project.getPackage()}.libs.${relocateFrom.split(".").last()}",
    configuration: String = "implementation",
    configurationAction: ExternalModuleDependency.() -> Unit = {}
): ExternalModuleDependency {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    project.relocate(relocateFrom, relocateTo)
    return addDependencyTo(dependencies, configuration, dependency, configurationAction)
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
    val version = SemanticVersion(minecraftVersion)
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
