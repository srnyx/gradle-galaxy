package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublishingExtension
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
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.data.pom.LicenseData
import xyz.srnyx.gradlegalaxy.data.pom.ScmData


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
 * 2. Creates a [MavenPublication] with the specified parameters
 * 3. Configures the [MavenPublication] with the specified [configuration]
 *
 * @param groupId The group ID
 * @param artifactId The artifact ID
 * @param version The version
 * @param component The [SoftwareComponent] to publish
 * @param artifacts The artifacts to publish
 * @param name The name of the project
 * @param description The description of the project
 * @param url The URL of the project
 * @param licenses The licenses of the project
 * @param developers The developers of the project
 * @param scm The SCM information of the project
 * @param configuration The configuration of the [MavenPublication]
 *
 * @return The [MavenPublication] that was created
 */
@Ignore
inline fun Project.setupPublishing(
    groupId: String? = null,
    artifactId: String? = null,
    version: String? = null,
    withJavadocSourcesJars: Boolean = true,
    component: SoftwareComponent? = components["java"],
    artifacts: Collection<Any> = emptyList(),
    name: String? = project.name,
    description: String? = project.description,
    url: String? = null,
    licenses: List<LicenseData> = emptyList(),
    developers: List<DeveloperData> = emptyList(),
    scm: ScmData? = null,
    crossinline configuration: MavenPublication.() -> Unit = {}
): MavenPublication {
    apply(plugin = "maven-publish")
    if (withJavadocSourcesJars) addJavadocSourcesJars()
    return (extensions["publishing"] as PublishingExtension).publications.create<MavenPublication>("maven") {
        groupId?.let { this.groupId = it }
        artifactId?.let { this.artifactId = it }
        version?.let { this.version = it }
        component?.let { this.from(component) }
        artifacts.forEach(this::artifact)
        pom {
            name?.let(this.name::set)
            description?.let(this.description::set)
            url?.let(this.url::set)
            licenses {
                licenses.forEach {
                    license {
                        this.name.set(it.name)
                        this.url.set(it.url)
                        it.distribution?.value?.let(this.distribution::set)
                        it.comments?.let(this.comments::set)
                    }
                }
            }
            developers {
                developers.filterNot(DeveloperData::isEmpty).forEach {
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
            if (scm != null) scm {
                connection.set(scm.connection)
                developerConnection.set(scm.developerConnection)
                scm.url?.let(this.url::set)
                scm.tag?.let(this.tag::set)
            }
        }
        configuration()
    }
}