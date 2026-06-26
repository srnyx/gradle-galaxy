package xyz.srnyx.gradlegalaxy.utility

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
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
import xyz.srnyx.gradlegalaxy.data.platforms.PluginPlatform
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository

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
fun Project.setupJava(
    config: JavaSetupConfig = JavaSetupConfig(),
) {
    this.group = config.group ?: this.group
    this.version = config.version
        ?: this.version.takeIf { it != Project.DEFAULT_VERSION }
        ?: when {
            inGitHubWorkflow() -> getEnvironmentVariable("GITHUB_REF_NAME")
                ?.takeIf { inGitHubRelease() }
                ?: getEnvironmentVariable("GITHUB_SHA")?.take(7)
            else -> null
        }
        ?: "dev"
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
 * 5. Calls [addPlatformsResourceFileTask] if enabled
 * 6. Calls [setupPublishingPlatforms] with the specified parameters
 *
 * @return The metadata for Annoying API if [MetadataConfig.useMetadata] is true, otherwise null
 */
@Used
fun Project.setupAnnoyingAPI(
    javaSetupConfig: JavaSetupConfig = JavaSetupConfig(),
    mcSetupConfig: MCSetupConfig = MCSetupConfig(),
    annoyingAPIConfig: DependencyConfig,
    annoyingSetupConfig: AnnoyingSetupConfig = AnnoyingSetupConfig(),
    metadataConfig: MetadataConfig = MetadataConfig(),
    customRuntimeLibrariesConfig: CustomRuntimeLibrariesConfig = CustomRuntimeLibrariesConfig(),
    publishingPlatformConfig: PublishingPlatformConfig = PublishingPlatformConfig(mapOf()),
): AnnoyingMetadata? {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    check(hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }

    // Setup Minecraft
    setupMC(javaSetupConfig, mcSetupConfig)

    // Get and process Annoying API metadata
    val metadata = metadataConfig.useMetadata.takeIf { it }?.let { getAnnoyingApiMetadata(annoyingAPIConfig.version) }
    if (metadata != null) {
        // Relocate Annoying API
        if (metadataConfig.relocateAnnoyingAPI) relocate(metadata.packageName)

        // Java version (only if custom not specified)
        if (metadataConfig.setJavaVersion && metadata.javaVersion != null && javaSetupConfig.javaVersion == null) {
            setJavaVersion(JavaVersion.toVersion(metadata.javaVersion))
        }

        // Repositories
        if (metadataConfig.addRepositories) metadata.repositories.forEach { repository(it) }

        // Runtime libraries
        processRuntimeLibraries(metadata.runtimeLibraries, metadataConfig.runtimeLibrariesConfig)
    }

    // Excludes
    if (metadataConfig.excludes) {
        val original = annoyingAPIConfig.configurationAction
        annoyingAPIConfig.configurationAction = {
            metadata?.excludes?.forEach { exclude(it.group, it.module) }
            original()
        }
    }

    // Add Annoying API
    annoyingAPI(annoyingAPIConfig)

    // Custom runtime libraries
    customRuntimeLibrariesConfig
        .takeIf { !it.runtimeLibraries.isEmpty() }
        ?.let { config ->
            // Process libraries
            config.processConfig?.let { processConfig ->
                processRuntimeLibraries(config.runtimeLibraries, processConfig)
            }

            // Generate enum
            config.generateRuntimeLibraryEnumConfig?.let { generateRuntimeLibraryEnumConfig ->
                generateAnnoyingApiRuntimeLibraryEnum(config.runtimeLibraries, generateRuntimeLibraryEnumConfig, metadata)
            }
        }

    // Platforms
    if (!publishingPlatformConfig.platforms.isEmpty()) {
        // Add resource file task
        if (annoyingSetupConfig.addPlatformsResourceFile) addPlatformsResourceFileTask(publishingPlatformConfig.platforms)

        // Setup publishing
        setupPublishingPlatforms(publishingPlatformConfig)
    }

    return metadata
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

    // Exclude opus-java if needed
    if (jdaSetupConfig.excludeOpus) {
        val original = jdaConfig.configurationAction
        jdaConfig.configurationAction = {
            exclude(module = "opus-java")
            original()
        }
    }

    // Add JDA with new jdaConfig (excludeOpus)
    jda(jdaConfig)

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
 * Sets up the project for testing with JUnit
 * 1. Adds the JUnit Jupiter and JUnit Platform dependencies
 * 2. Configures the test task to use JUnit Platform
 *
 * @param junitBomConfig The configuration for the JUnit BOM dependency
 * @param configuration The configuration for the test task
 *
 * @return The test task that was configured
 */
fun Project.setupTesting(
    junitBomConfig: DependencyConfig,
    block: Test.() -> Unit = {},
): TaskProvider<Test> {
    // Add dependencies
    dependencies {
        (junitBomConfig.configurations ?: listOf("testImplementation")).forEach { configurationName ->
            addDependencyTo(this, configurationName, platform("org.junit:junit-bom:${junitBomConfig.version}"), junitBomConfig.configurationAction)
        }
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    // Configure and return test task
    return tasks.named<Test>("test") {
        useJUnitPlatform()

        // For ByteBuddy/Mockito/MockBukkit
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")

        block()
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
fun Project.setupMockBukkit(
    junitBomConfig: DependencyConfig,
    mockBukkitDependencyConfig: DependencyConfig,
    mockBukkitConfig: MockBukkitConfig = MockBukkitConfig(),
    block: Test.() -> Unit = {},
): TaskProvider<Test> {
    // Add MockBukkit
    mockBukkit(mockBukkitDependencyConfig, mockBukkitConfig)

    // Exclude spigot-api from test classpath so MockBukkit's Paper takes precedence
    configurations.named("testImplementation") { exclude("org.spigotmc", "spigot-api") }

    // Setup testing
    return setupTesting(junitBomConfig, block)
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
fun Project.setupPublishingSimple(
    config: PublishingSimpleConfig = PublishingSimpleConfig(this),
    configuration: MavenPublication.() -> Unit = {},
): MavenPublication {
    apply(plugin = "maven-publish")

    // Javadocs and sources
    if (config.withJavadocSourcesJars) addJavadocSourcesJars()

    // Silence missing Javadoc warnings
    if (config.silenceMissingJavadocWarnings) silenceMissingJavaDocWarnings()

    // Create publication
    return getPublishing().publications.create<MavenPublication>("maven") {
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

        configuration()
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

    // Create publication
    return setupPublishingSimple(simpleConfig)
}

/**
 * Sets up publishing for project platforms (GitHub, Modrinth, CurseForge)
 *
 * @param config The configuration for setting up publishing for project platforms
 * @param gitHubConfig The configuration for setting up publishing for GitHub
 * @param modrinthConfig The configuration for setting up publishing for Modrinth
 * @param curseForgeConfig The configuration for setting up publishing for CurseForge
 * @param action The action to perform after setting up publishing for project platforms
 */
fun Project.setupPublishingPlatforms(
    config: PublishingPlatformConfig,
    modrinthAction: Action<Modrinth> = Action {},
    curseForgeAction: Action<Curseforge> = Action {},
    action: Action<ModPublishExtension> = Action {},
) {
    check(hasModPublishPlugin()) { "Mod Publish plugin is not applied!" }

    val minecraftVersionEnd = config.minecraftVersionEnd ?: "latest"

    // Ensure publishing runs after building
    tasks.withType<PublishModTask> { dependsOn(if (hasShadowPlugin()) "shadowJar" else "jar") }

    // Setup publishing
    extensions.configure<ModPublishExtension>("publishMods") {
        displayName.set(project.version.toString())
        modLoaders.set(config.loaders)

        // Type
        type.set(if (inGitHubRelease()) ReleaseType.STABLE else ReleaseType.ALPHA)

        // Primary file (shadowJar or jar)
        file.set(tasks.named<Jar>(if (hasShadowPlugin()) "shadowJar" else "jar").flatMap { it.archiveFile })

        // Additional files (javadocJar and sourcesJar)
        tasks.findByName("javadocJar")?.let { additionalFiles.from(it) }
        tasks.findByName("sourcesJar")?.let { additionalFiles.from(it) }

        // Changelog
        // File exists: file contents
        // In GitHub workflow:
        //   Non-STABLE: "github.com/REPO/commit/SHA"
        //   STABLE: release link
        // Else: "No changelog specified"
        val changelogFile = file("Changelogs/${project.version}.md")
        changelog.set(when {
            // File
            changelogFile.exists() -> changelogFile.readText()

            inGitHubWorkflow() -> run {
                val gitHubRepository = getEnvironmentVariable("GITHUB_REPOSITORY") ?: return@run "No changelog specified"
                val githubLink = "https://github.com/${gitHubRepository}"

                // Non-STABLE: commit SHA
                if (type.get() != ReleaseType.STABLE) return@run "${githubLink}/commit/${getEnvironmentVariable("GITHUB_SHA")}"

                // STABLE: release link
                "${githubLink}/releases/tag/${project.version}"
            }

            else -> "No changelog specified"
        })

        // Modrinth
        val modrinthIdentifier = config.platforms[PluginPlatform.MODRINTH]
        if (modrinthIdentifier != null) {
            val token = getEnvironmentVariable("MODRINTH_TOKEN")
            if (token != null) modrinth {
                accessToken.set(token)
                minecraftVersionRange {
                    start.set(config.minecraftVersionStart)
                    end.set(minecraftVersionEnd)
                }

                projectId.set(modrinthIdentifier)
                modrinthAction.execute(this)
            }
        }

        // CurseForge
        val curseForgeIdentifier = config.platforms[PluginPlatform.CURSEFORGE]
        if (curseForgeIdentifier != null) {
            val token = getEnvironmentVariable("CURSEFORGE_TOKEN")
            if (token != null) curseforge {
                accessToken.set(getEnvironmentVariable("CURSEFORGE_TOKEN"))
                minecraftVersionRange {
                    start.set(config.minecraftVersionStart)
                    end.set(minecraftVersionEnd)
                }
                server.set(true)

                projectId.set(curseForgeIdentifier)
                curseForgeAction.execute(this)
            }
        }

        action(this)
    }
}
