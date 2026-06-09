package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.JdaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.MCSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.AnnoyingSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.MetadataConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingEnvConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingSimpleConfig
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository

import kotlin.String
import kotlin.text.replace


/**
 * Sets up the project for a simple Java project
 *
 * 1. Sets up the project with the specified `group` and `version` for a simple Java project
 * 2. Calls [setJavaVersion], [setTextEncoding], and [addReplacementsTask]
 * 3. If the [shadow plugin is applied][hasShadowPlugin], it will also call [setShadowArchiveClassifier] and [addJavadocSourcesJars]
 *
 * @param config The configuration for setting up Java
 */
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
 *
 * @param javaSetupConfig The configuration for [setupJava]
 * @param mcSetupConfig The configuration for [setupMC]
 * @param annoyingAPIConfig The configuration for [annoyingAPI]
 */
@Used
fun Project.setupAnnoyingAPI(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    mcSetupConfig: MCSetupConfig = MCSetupConfig(),
    annoyingSetupConfig: AnnoyingSetupConfig = AnnoyingSetupConfig(),
    annoyingAPIConfig: DependencyConfig,
) {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    check(hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }

    // Setup Minecraft
    setupMC(javaSetupConfig, mcSetupConfig)

    // Get and process Annoying API metadata
    val metadata = annoyingSetupConfig.metadataConfig.useMetadata.takeIf { it }?.let { getAnnoyingApiMetadata(annoyingAPIConfig.version) }
    if (metadata != null) {
        // Relocate Annoying API
        if (annoyingSetupConfig.metadataConfig.relocateAnnoyingAPI) relocate(metadata.packageName)

        // Java version (only if custom not specified)
        if (annoyingSetupConfig.metadataConfig.setJavaVersion && metadata.javaVersion != null && javaSetupConfig.javaVersion == null) {
            setJavaVersion(JavaVersion.toVersion(metadata.javaVersion))
        }

        // Repositories
        if (annoyingSetupConfig.metadataConfig.addRepositories) metadata.repositories.forEach { repository(it) }

        // Runtime libraries
        val getPackage = getPackage()
        metadata.runtimeLibraries.forEach { library ->
            // Add repositories
            if (annoyingSetupConfig.metadataConfig.runtimeLibrariesConfig.addRepositories) library.repositories.forEach { repo -> repository(repo) }

            // Add dependency
            if (annoyingSetupConfig.metadataConfig.runtimeLibrariesConfig.addDependencies) {
                dependencies.add("compileOnly", "${library.group}:${library.name}:${library.version}")
            }

            // Relocations
            if (annoyingSetupConfig.metadataConfig.runtimeLibrariesConfig.relocate) library.relocations.forEach { relocation ->
                val to = relocation.to?.replace("{package}", getPackage)
                if (to != null) {
                    relocate(relocation.from, to)
                } else {
                    relocate(relocation.from)
                }
            }
        }
    }

    if (annoyingSetupConfig.metadataConfig.excludes) {
        val original = annoyingAPIConfig.configurationAction
        annoyingAPIConfig.configurationAction = {
            metadata?.excludes?.forEach { exclude(it.group, it.module) }
            original()
        }
    }

    // Add Annoying API
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
@Used
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
        config.textArtifacts.forEach { textArtifact ->
            val taskName = "generate${textArtifact.classifier.capitalized()}TextArtifact"
            val extensionSuffix = textArtifact.extension?.let { ".$it" } ?: ""
            val outputFile = layout.buildDirectory.file("generated/publications/${this.artifactId}-${this.version}-${textArtifact.classifier}$extensionSuffix")

            val textProvider = project.provider { textArtifact.text.invoke() }
            val task = tasks.register(taskName) {
                group = "publishing"
                description = "Generates the ${textArtifact.classifier} artifact for publication ${this.name}"

                inputs.property("text", textProvider)
                outputs.file(outputFile)

                doLast {
                    outputFile.get().asFile.apply {
                        parentFile.mkdirs()
                        writeText(textProvider.get())
                    }
                }
            }

            artifact(outputFile) {
                this.classifier = textArtifact.classifier
                this.extension = textArtifact.extension
                builtBy(task)
            }
        }
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
@Used
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
