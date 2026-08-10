package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test
import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.JdaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.MCSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.AnnoyingSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.CustomRuntimeLibrariesConfig
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.MetadataConfig
import xyz.srnyx.gradlegalaxy.data.config.dependency.MockBukkitConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingEnvConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingPlatformConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingSimpleConfig
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.extensions.GradleGalaxyExtension


/**
 * Sets up the project for a simple Java project
 *
 * 1. Sets up the project with the specified `group` and `version` for a simple Java project
 * 2. Calls [setJavaVersion], [setTextEncoding], and [addReplacementsTask]
 * 3. If the [shadow plugin is applied][hasShadowPlugin], it will also call [setShadowArchiveClassifier] and [addJavadocSourcesJars]
 *
 * @param config The configuration for setting up Java
 */
@Deprecated("Use galaxy { setup { java { ... } } } instead")
fun Project.setupJava(
    config: JavaSetupConfig = JavaSetupConfig(),
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup.java(config.toExtension())
    }
}

/**
 * Sets up the project for Minecraft development
 *
 * 1. Calls [setupJava] with the specified parameters
 * 2. Calls [addReplacementsTask] with the specified parameters
 *
 * @param javaSetupConfig The configuration for [setupJava]
 * @param mcSetupConfig The configuration for Minecraft setup
 */
@Deprecated("Use galaxy { setup { minecraft { ... } } } instead")
fun Project.setupMC(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    mcSetupConfig: MCSetupConfig = MCSetupConfig(),
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            java(javaSetupConfig.toExtension())
            minecraft(mcSetupConfig.toExtension())
        }
    }
}

/**
 * Sets up the project using Annoying API. **The [root project's name][Project.getName] must be the same as the one in plugin.yml!**
 *
 * 1. Checks if the Java and Shadow plugins are applied
 * 2. Adds srnyx's repositories and [Repository.ALESSIO_DP] for Libby
 * 2. Gets and processes Annoying API metadata (if [MetadataConfig.useMetadata] is true)
 *    1. Sets Java version if specified
 *    2. Adds repositories
 *    3. For each runtime library:
 *       1. Adds repositories
 *       2. Adds dependency
 *       3. Adds relocations
 *    4. Excludes some Annoying API dependencies
 * 3. Calls [setupMC] with the specified parameters
 * 4. Calls [annoyingAPI] with the specified parameters
 * 5. Calls [addPlatformsResourceFileTask] if enabled
 * 6. Calls [setupPublishingPlatforms] with the specified parameters
 *
 * @return The metadata for Annoying API if [MetadataConfig.useMetadata] is true, otherwise null
 */
@Deprecated("Use galaxy { setup { annoyingAPI { ... } } } instead")
fun Project.setupAnnoyingAPI(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    mcSetupConfig: MCSetupConfig = MCSetupConfig(),
    annoyingAPIConfig: DependencyConfig,
    annoyingSetupConfig: AnnoyingSetupConfig = AnnoyingSetupConfig(),
    metadataConfig: MetadataConfig = MetadataConfig(),
    customRuntimeLibrariesConfig: CustomRuntimeLibrariesConfig = CustomRuntimeLibrariesConfig(),
    publishingPlatformConfig: PublishingPlatformConfig = PublishingPlatformConfig(mapOf()),
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            java(javaSetupConfig.toExtension())
            minecraft(mcSetupConfig.toExtension())
            annoyingAPI {
                metadata(metadataConfig.toExtension(customRuntimeLibrariesConfig.runtimeLibraries))
                customRuntimeLibraries(customRuntimeLibrariesConfig.toExtension())
            }
            publishing {
                platforms(publishingPlatformConfig.platforms) {
                    publishingPlatformConfig.toExtension()(this)
                    addResourceFile = annoyingSetupConfig.addPlatformsResourceFile
                }
            }
            runPaper {}
        }

        dependencies {
            annoyingAPI(annoyingAPIConfig.version, annoyingAPIConfig.toExtension())
        }
    }
}

/**
 * Sets up the project using JDA (Java-Discord API)
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupJava] with the specified parameters
 * 3. Calls [setMainClass] with the specified main class name
 * 4. Adds the `-parameters` compiler argument using [addCompilerArgs]
 * 5. Calls [jda] (excluding `opus-java` if specified)
 * 6. Fixes some tasks to depend on the correct jar tasks
 *
 * @param javaSetupConfig The configuration for [setupJava]
 * @param jdaSetupConfig The configuration for JDA setup
 * @param jdaConfig The configuration for [jda]
 */
@Deprecated("Use galaxy { setup { jda { ... } } } instead")
fun Project.setupJda(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    jdaSetupConfig: JdaSetupConfig = JdaSetupConfig(),
    jdaConfig: DependencyConfig,
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            java(javaSetupConfig.toExtension())
        }

        dependencies {
            jda(jdaConfig.version, jdaConfig.toExtension())
        }
    }
}

/**
 * Sets up the project using Lazy Library
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupJda] with the specified parameters
 * 3. Calls [lazyLibrary] with the specified parameters
 *
 * @param javaSetupConfig The configuration for [setupJava]
 * @param jdaSetupConfig The configuration for JDA setup
 * @param jdaConfig The configuration for [jda]
 * @param lazyLibraryConfig The configuration for [lazyLibrary]
 */
@Deprecated("Use galaxy { setup { lazyLibrary { ... } } } instead")
fun Project.setupLazyLibrary(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    jdaSetupConfig: JdaSetupConfig = JdaSetupConfig(),
    jdaConfig: DependencyConfig,
    lazyLibraryConfig: DependencyConfig,
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            java(javaSetupConfig.toExtension())
            jda(jdaSetupConfig.toExtension())
        }

        dependencies {
            jda(jdaConfig.version, jdaConfig.toExtension())
            lazyLibrary(lazyLibraryConfig.version, lazyLibraryConfig.toExtension())
        }
    }
}

/**
 * Sets up the project for testing with JUnit
 * 1. Adds the JUnit Jupiter and JUnit Platform dependencies
 * 2. Configures the test task to use JUnit Platform
 *
 * @param junitBomConfig The configuration for the JUnit BOM dependency
 *
 * @return The test task that was configured
 */
@Deprecated("Use galaxy { setup { testing { ... } } } instead")
fun Project.setupTesting(
    junitBomConfig: DependencyConfig,
    block: Test.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            testing {
                jUnit {
                    action(block)
                }
            }
        }

        dependencies {
            jUnitBom(junitBomConfig.version, junitBomConfig.toExtension())
        }
    }
}

/**
 * Sets up the project for testing with JUnit and MockBukkit
 *
 * 1. Calls [mockBukkit] with the specified parameters
 * 2. Excludes `org.spigotmc:spigot-api` from test classpath
 * 3. Calls [setupTesting] with the specified parameters
 *
 * @param junitBomConfig The configuration for the JUnit BOM dependency
 * @param mockBukkitDependencyConfig The configuration for the MockBukkit dependency
 * @param mockBukkitConfig The configuration for MockBukkit
 * @param block The configuration for the test task
 *
 * @return The test task that was configured
 */
@Deprecated("Use galaxy { setup { mockBukkit { ... } } } instead")
fun Project.setupMockBukkit(
    junitBomConfig: DependencyConfig,
    mockBukkitDependencyConfig: DependencyConfig,
    mockBukkitConfig: MockBukkitConfig = MockBukkitConfig(),
    block: Test.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            testing {
                jUnit {
                    action(block)
                }
            }
        }

        dependencies {
            jUnitBom(junitBomConfig.version, junitBomConfig.toExtension())
            mockBukkit(mockBukkitDependencyConfig.version) {
                mockBukkitDependencyConfig.toExtension()(this)
                mockBukkitConfig.toExtension()(this)
            }
        }
    }
}

/**
 * Sets up a simple publishing configuration
 *
 * 1. Applies the `maven-publish` plugin
 * 2. If `withJavadocSourcesJars` is true: call [addJavadocSourcesJars]
 * 3. Creates a [MavenPublication] with the specified [config]
 *
 * @param config The configuration for setting up publishing
 * @param configuration The configuration for the publication
 *
 * @return The [MavenPublication] that was created
 */
@Deprecated("Use galaxy { setup { publishing { simple { ... } } } } instead")
fun Project.setupPublishingSimple(
    config: PublishingSimpleConfig = PublishingSimpleConfig(this),
    configuration: MavenPublication.() -> Unit = {},
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            publishing {
                simple {
                    config.toExtension()(this)
                    publication(configuration)
                }
            }
        }
    }
}

/**
 * Sets up a bit more advanced publishing configuration using environment variables and a custom repository
 *
 * 1. Applies the `maven-publish` plugin
 * 2. Creates a repository with the specified maven URL and credential environment variables
 * 3. Calls [setupPublishingSimple] with the specified parameters
 *
 * @param simpleConfig The configuration for setting up publishing using a simple configuration
 * @param envConfig The configuration for setting up publishing using environment variables
 *
 * @return The [MavenPublication] that was created
 */
@Deprecated("Use galaxy { setup { publishing { env { ... } } } } instead")
fun Project.setupPublishingEnv(
    simpleConfig: PublishingSimpleConfig = PublishingSimpleConfig(this),
    envConfig: PublishingEnvConfig = PublishingEnvConfig(),
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            publishing {
                simple(simpleConfig.toExtension())
                env(envConfig.toExtension())
            }
        }
    }
}

/**
 * Sets up publishing for project platforms (GitHub, Modrinth, CurseForge, Hangar)
 *
 * @param config The configuration for setting up publishing for project platforms
 */
@Deprecated("Use galaxy { setup { publishing { platforms { ... } } } instead")
fun Project.setupPublishingPlatforms(
    config: PublishingPlatformConfig,
) {
    project.extensions.configure<GradleGalaxyExtension>("galaxy") {
        setup {
            publishing {
                platforms(config.platforms, config.toExtension())
            }
        }
    }
}
