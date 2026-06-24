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
import xyz.srnyx.gradlegalaxy.enums.PaperVersion
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository


/**
 * 1. Sets the Java version for the project depending on the version
 * 2. Adds the [Repository.SONATYPE_SNAPSHOTS_OLD] repository if the version is 1.15 or below
 * 3. Adds the [Repository.MAVEN_CENTRAL] and [Repository.SPIGOT] repositories
 * 4. Adds the Spigot-API dependency (org.spigotmc:spigot-api:[getVersionString])
 *
 * @param config The configuration for the Spigot-API dependency
 */
@Used
fun Project.spigotAPI(
    config: DependencyConfig,
    spigotConfig: SpigotConfig = SpigotConfig(),
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Java version
    if (spigotConfig.setJavaVersion) setJavaVersion(getJavaVersionForMC(config.version))

    // Repositories
    val semanticVersion = SemanticVersion(config.version)
    if (semanticVersion.major <= 1 && semanticVersion.minor <= 15) repository(Repository.SONATYPE_SNAPSHOTS_OLD)
    repository(Repository.MAVEN_CENTRAL, Repository.SPIGOT, Repository.SPIGOT_SNAPSHOTS)

    // Add dependency
    return (config.configurations ?: listOf("compileOnly", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "org.spigotmc:spigot-api:${getVersionString(config.version)}") {
            config.configurationAction(this)
            block()
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], maven local, and [Repository.SPIGOT] repositories
 * 2. Adds the Spigot dependency (org.spigotmc:spigot:[getVersionString])
 *
 * @param config The configuration for the Spigot dependency
 */
@Used
fun Project.spigotNMS(
    config: DependencyConfig,
    spigotConfig: SpigotConfig = SpigotConfig(),
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Java version
    if (spigotConfig.setJavaVersion) setJavaVersion(getJavaVersionForMC(config.version))

    // Repositories
    repository(Repository.MAVEN_CENTRAL, Repository.SPIGOT, Repository.SPIGOT_SNAPSHOTS)
    repositories.mavenLocal()

    // Add dependency
    return (config.configurations ?: listOf("compileOnly", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "org.spigotmc:spigot:${getVersionString(config.version)}") {
            config.configurationAction(this)
            block()
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL], [Repository.SONATYPE_SNAPSHOTS_OLD], and [Repository.PAPER] repositories
 * 2. Adds the Paper dependency ([PaperVersion.groupId]:[PaperVersion.artifactId]:`version`-R0.1-SNAPSHOT)
 *
 * @param config The configuration for the Paper dependency
 */
@Used
fun Project.paper(
    config: DependencyConfig,
    spigotConfig: SpigotConfig = SpigotConfig(),
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Java version
    if (spigotConfig.setJavaVersion) setJavaVersion(getJavaVersionForMC(config.version))

    // Repositories
    repository(Repository.MAVEN_CENTRAL, Repository.SONATYPE_SNAPSHOTS_OLD, Repository.PAPER)

    // Add dependency
    val paperVersion: PaperVersion = PaperVersion.parse(config.version)
    return (config.configurations ?: listOf("compileOnly", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "${paperVersion.groupId}:${paperVersion.artifactId}:${getVersionString(config.version)}") {
            config.configurationAction(this)
            block()
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
    dependencies.forEach { addDependencyTo(project.dependencies, it.config.configurations?.firstOrNull() ?: configurationAll ?: "implementation", "net.kyori:${it.component.getComponent()}:${it.config.version}", it.config.configurationAction) }
}

/**
 * 1. Adds srnyx's repositories and [Repository.ALESSIO_DP] (for Libby) repositories
 * 2. Relocates `xyz.srnyx.annoyingapi`
 * 3. Adds the dependency to the provided Annoying API version
 *
 * @param config The configuration for the Annoying API dependency
 *
 * @return The [ExternalModuleDependency] of the Annoying API dependency
 */
fun Project.annoyingAPI(
    config: DependencyConfig,
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }

    // Add srnyx's repositories
    repository(Repository.SRNYX_RELEASES, Repository.SRNYX_SNAPSHOTS)

    // Add Annoying API dependency
    return (config.configurations ?: listOf("implementation", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "xyz.srnyx:annoying-api:${config.version}") {
            config.configurationAction(this)
            block()
        }
    }
}

/**
 * 1. Adds the [Repository.MAVEN_CENTRAL] repository
 * 2. Adds the dependency to the provided JDA version
 *
 * @param config The configuration for the JDA dependency
 */
fun Project.jda(
    config: DependencyConfig,
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Repositories
    repository(Repository.MAVEN_CENTRAL)

    // Add dependency
    return (config.configurations ?: listOf("implementation", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "net.dv8tion:JDA:${config.version}") {
            config.configurationAction(this)
            block()
        }
    }
}

/**
 * 1. Adds srnyx's repository
 * 2. Adds the dependency to the provided Lazy Library version
 *
 * @param config The configuration for the Lazy Library dependency
 */
fun Project.lazyLibrary(
    config: DependencyConfig,
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Repositories
    repository(Repository.SRNYX_RELEASES, Repository.SRNYX_SNAPSHOTS)

    // Add dependency
    return (config.configurations ?: listOf("implementation", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "xyz.srnyx:lazy-library:${config.version}") {
            config.configurationAction(this)
            block()
        }
    }
}

/**
 * 1. Adds srnyx's repository
 * 2. Adds the dependency to the provided Magic Mongo version
 *
 * @param config The configuration for the Magic Mongo dependency
 */
@Used
fun Project.magicMongo(
    config: DependencyConfig,
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Repositories
    repository(Repository.SRNYX_RELEASES, Repository.SRNYX_SNAPSHOTS)

    // Add dependency
    return (config.configurations ?: listOf("implementation", "testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "xyz.srnyx:magic-mongo:${config.version}") {
            config.configurationAction(this)
            block()
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
 *
 * @return The [ExternalModuleDependency]s of the MockBukkit dependency
 */
fun Project.mockBukkit(
    config: DependencyConfig,
    mockBukkitConfig: MockBukkitConfig = MockBukkitConfig(),
    block: ExternalModuleDependency.() -> Unit = {},
): List<ExternalModuleDependency> {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Repositories
    repository(Repository.MAVEN_CENTRAL, Repository.PAPER)

    // Add dependency
    return (config.configurations ?: listOf("testImplementation")).map { configuration ->
        addDependencyTo(dependencies, configuration, "${mockBukkitConfig.group}:MockBukkit-v${mockBukkitConfig.minecraftVersion}:${config.version}") {
            config.configurationAction(this)
            block()
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
