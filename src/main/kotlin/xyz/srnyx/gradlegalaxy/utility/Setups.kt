package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.JdaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.MCSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.PublishingEnvConfig
import xyz.srnyx.gradlegalaxy.data.config.PublishingSimpleConfig
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData

import kotlin.String


/**
 * Sets up the project for a simple Java project
 *
 * 1. Sets up the project with the specified `group` and `version` for a simple Java project
 * 2. Calls [setJavaVersion], [setTextEncoding], and [addReplacementsTask]
 * 3. If the [shadow plugin is applied][hasShadowPlugin], it will also call [setShadowArchiveClassifier] and [addJavadocSourcesJars]
 *
 * @param config The configuration for setting up Java
 */
@Ignore
fun Project.setupJava(
    config: JavaSetupConfig = JavaSetupConfig(),
) {
    this.group = config.group ?: this.group
    this.version = config.version ?: this.version
    this.description = config.description ?: this.description
    config.javaVersion?.let(::setJavaVersion)
    config.textEncoding?.let(::setTextEncoding)
    if (hasShadowPlugin()) {
        config.archiveClassifier?.let(::setShadowArchiveClassifier)
        addBuildShadowTask()
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
@Ignore
fun Project.setupMC(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    mcSetupConfig: MCSetupConfig = MCSetupConfig(),
) {
    setupJava(javaSetupConfig)
    if (mcSetupConfig.replacementFiles != null && mcSetupConfig.replacements != null) addReplacementsTask(mcSetupConfig.replacementFiles, mcSetupConfig.replacements)
}

/**
 * Sets up the project using Annoying API. **The [root project's name][Project.getName] must be the same as the one in plugin.yml!**
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupMC] with the specified parameters
 * 3. Calls [annoyingAPI] with the specified parameters
 *
 * @param javaSetupConfig The configuration for [setupJava]
 * @param mcSetupConfig The configuration for [setupMC]
 * @param annoyingAPIConfig The configuration for [annoyingAPI]
 */
@Ignore
fun Project.setupAnnoyingAPI(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    mcSetupConfig: MCSetupConfig = MCSetupConfig(),
    annoyingAPIConfig: DependencyConfig,
) {
    check(hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }
    setupMC(javaSetupConfig, mcSetupConfig)
    annoyingAPI(annoyingAPIConfig)
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
fun Project.setupJda(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    jdaSetupConfig: JdaSetupConfig = JdaSetupConfig(),
    jdaConfig: DependencyConfig,
) {
    check(hasShadowPlugin()) { "Shadow plugin is required for JDA!" }
    setupJava(javaSetupConfig)
    setMainClass(jdaSetupConfig.mainClassName)
    addCompilerArgs("-parameters")
    jda(jdaConfig)

    // Exclude opus-java if needed
    if (jdaSetupConfig.excludeOpus) {
        val original = jdaConfig.configurationAction
        jdaConfig.configurationAction = {
            exclude(module = "opus-java")
            original()
        }
    }

    // Fix some tasks
    tasks["distZip"].dependsOn("shadowJar")
    tasks["distTar"].dependsOn("shadowJar")
    tasks["startScripts"].dependsOn("shadowJar")
    tasks["startShadowScripts"].dependsOn("jar")
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
@Ignore
fun Project.setupLazyLibrary(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    jdaSetupConfig: JdaSetupConfig = JdaSetupConfig(),
    jdaConfig: DependencyConfig,
    lazyLibraryConfig: DependencyConfig,
) {
    check(hasShadowPlugin()) { "Shadow plugin is required for Lazy Library!" }
    setupJda(javaSetupConfig, jdaSetupConfig, jdaConfig)
    lazyLibrary(lazyLibraryConfig)
}

/**
 * Sets up a simple publishing configuration
 *
 * 1. Applies the `maven-publish` plugin
 * 2. If `withJavadocSourcesJars` is true: call [addJavadocSourcesJars]
 * 3. Creates a [MavenPublication] with the specified [config]
 *
 * @param config The configuration for setting up publishing
 *
 * @return The [MavenPublication] that was created
 */
@Ignore
fun Project.setupPublishingSimple(
    config: PublishingSimpleConfig = PublishingSimpleConfig(this),
): MavenPublication {
    apply(plugin = "maven-publish")
    if (config.withJavadocSourcesJars) addJavadocSourcesJars()

    val publication: MavenPublication = getPublishing().publications.create<MavenPublication>("maven") {
        config.groupId?.let { this.groupId = it }
        config.artifactId?.let { this.artifactId = it }
        config.version?.let { this.version = it }
        config.component?.let { this.from(config.component) }
        config.artifacts.forEach(this::artifact)
        pom {
            config.name?.let(this.name::set)
            config.description?.let(this.description::set)
            config.url?.let(this.url::set)
            licenses {
                config.licenses.forEach {
                    license {
                        this.name.set(it.name)
                        this.url.set(it.url)
                        it.distribution?.value?.let(this.distribution::set)
                        it.comments?.let(this.comments::set)
                    }
                }
            }
            developers {
                config.developers.filterNot(DeveloperData::isEmpty).forEach {
                    developer {
                        it.id?.let(this.id::set)
                        it.name?.let(this.name::set)
                        it.url?.let(this.url::set)
                        it.email?.let(this.email::set)
                        it.timezone?.let(this.timezone::set)
                        it.organization?.let(this.organization::set)
                        it.organizationUrl?.let(this.organizationUrl::set)
                        it.roles.takeIf(List<String>::isNotEmpty)?.let(this.roles::set)
                        it.properties.takeIf(Map<String, String>::isNotEmpty)?.let(this.properties::set)
                    }
                }
            }
            if (config.scm != null) scm {
                connection.set(config.scm.connection)
                developerConnection.set(config.scm.developerConnection)
                config.scm.url?.let(this.url::set)
                config.scm.tag?.let(this.tag::set)
            }
        }
    }

    config.configuration(publication)
    return publication
}

/**
 * Sets up a bit more advanced publishing configuration using environment variables and a custom repository
 *
 * 1. Applies the `maven-publish` plugin
 * 2. Creates a repository with the specified maven URL and credential environment variables
 * 3. Updates the version using the specified environment variables or the default version
 * 4. Calls [setupPublishingSimple] with the specified parameters
 *
 * @param simpleConfig The configuration for setting up publishing using a simple configuration
 * @param envConfig The configuration for setting up publishing using environment variables
 *
 * @return The [MavenPublication] that was created
 */
@Ignore
fun Project.setupPublishingEnv(
    simpleConfig: PublishingSimpleConfig = PublishingSimpleConfig(this),
    envConfig: PublishingEnvConfig = PublishingEnvConfig(),
): MavenPublication {
    apply(plugin = "maven-publish")

    // Create repository
    val resolvedMavenUrl = envConfig.mavenUrl ?: getEnvironmentVariable(envConfig.mavenUrlEnv)
    if (resolvedMavenUrl != null) getPublishing().repositories.maven {
        url = uri(resolvedMavenUrl)

        val usernameEnv = getEnvironmentVariable(envConfig.usernameEnv)
        val passwordEnv = getEnvironmentVariable(envConfig.passwordEnv)
        if (usernameEnv != null || passwordEnv != null) credentials {
            if (usernameEnv != null) username = usernameEnv
            if (passwordEnv != null) password = passwordEnv
        }
    }

    // Set version
    if (simpleConfig.version == null) simpleConfig.version = getEnvironmentVariable(envConfig.versionEnv) ?: envConfig.defaultVersion

    // Create publication
    return setupPublishingSimple(simpleConfig)
}