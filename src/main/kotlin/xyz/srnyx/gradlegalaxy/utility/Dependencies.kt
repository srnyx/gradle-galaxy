package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import xyz.srnyx.gradlegalaxy.data.AdventureDependency
import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.dependency.MockBukkitConfig
import xyz.srnyx.gradlegalaxy.data.config.dependency.SpigotConfig
import xyz.srnyx.gradlegalaxy.extensions.GradleGalaxyExtension
import xyz.srnyx.gradlegalaxy.extensions.Phase
import xyz.srnyx.gradlegalaxy.extensions.Repositories
import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension


/**
 * @see MinecraftExtension.spigotAPI
 */
@Deprecated("Use galaxy { minecraft { spigotAPI(version) { ... } } } instead")
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
 * @see MinecraftExtension.spigotNMS
 */
@Deprecated("Use galaxy { minecraft { spigotNMS(version) { ... } } } instead")
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
 * 1. Adds the [Repositories.MAVEN_CENTRAL], [Repositories.SONATYPE_SNAPSHOTS_OLD], and [Repositories.PAPER] repositories
 * 2. Adds the Paper dependency (`group`:`artifact`:`version`-R0.1-SNAPSHOT)
 *
 * @param config The configuration for the Paper dependency
 */
@Deprecated("Use galaxy { minecraft { paper(version) { ... } } } instead")
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
 * 1. Adds the [Repositories.MAVEN_CENTRAL] repository
 * 2. Adds the dependencies to the provided Adventure components
 *
 * @param dependencies The Adventure dependencies to add
 * @param configurationAll The configuration to use for the dependencies if they don't have one specified
 */
@Deprecated("Use galaxy { minecraft { adventure { ... } } } instead")
fun Project.adventure(vararg dependencies: AdventureDependency, configurationAll: String? = null) {
    // configurationAll
    if (configurationAll != null) dependencies.forEach { dependency ->
        if (dependency.config.configurations == null) dependency.config.configurations = listOf(configurationAll)
    }

    extensions.configure<GradleGalaxyExtension>("galaxy") {
        deferred.defer(Phase.FINALIZE) { dependencies.forEach { it.toExtension(objects).add(project) } }
    }
}

/**
 * 1. Adds srnyx's repositories and [Repositories.ALESSIO_DP] (for Libby) repositories
 * 2. Relocates `xyz.srnyx.annoyingapi`
 * 3. Adds the dependency to the provided Annoying API version
 *
 * @param config The configuration for the Annoying API dependency
 */
@Deprecated("Use galaxy { minecraft { annoyingAPI(version) { ... } } } instead")
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
 * 1. Adds the [Repositories.MAVEN_CENTRAL] repository
 * 2. Adds the dependency to the provided JDA version
 *
 * @param config The configuration for the JDA dependency
 */
@Deprecated("Use galaxy { discord { jda(version) { ... } } } instead")
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
@Deprecated("Use galaxy { discord { lazyLibrary(version) { ... } } } instead")
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
 * 1. Adds [Repositories.MAVEN_CENTRAL] and [Repositories.PAPER] repositories
 * 2. Adds the dependency to the provided MockBukkit version
 *
 * @param config The configuration for the MockBukkit dependency
 * @param mockBukkitConfig The configuration for MockBukkit
 * @param block The block to apply to the dependency
 */
@Deprecated("Use galaxy { testing { mockBukkit(version) { ... } } } instead")
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
@Deprecated("Use galaxy { dependency { add(...) } } instead")
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
@Deprecated("Use galaxy { dependency { add(...) } } instead")
fun Project.dependencyRelocate(
    dependency: String,
    relocateFrom: String = dependency.split(":").first(),
    relocateTo: String = "${project.getPackage()}.libs.${relocateFrom.split(".").last()}",
    configuration: String = "implementation",
    configurationAction: ModuleDependency.() -> Unit = {}
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        dependency {
            add(dependency) {
                configurations.set(listOf(configuration))
                action(configurationAction)
                relocate(relocateFrom, relocateTo)
            }
        }
    }
}
