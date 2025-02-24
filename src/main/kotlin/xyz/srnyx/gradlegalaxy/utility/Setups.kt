package xyz.srnyx.gradlegalaxy.utility

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.data.pom.LicenseData
import xyz.srnyx.gradlegalaxy.data.pom.ScmData


/**
 * Sets up the project with the specified [group] and [version] for a simple Java project
 *
 * Calls [setJavaVersion], [setTextEncoding], and [addReplacementsTask]
 *
 * If the [shadow plugin is applied][hasShadowPlugin], it will also call [setShadowArchiveClassifier] and [addJavadocSourcesJars]
 *
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 * @param archiveClassifier The archive classifier for the [shadow jar][setShadowArchiveClassifier]
 */
@Ignore
fun Project.setupJava(
    group: String = project.group.toString(),
    version: String = project.version.toString(),
    description: String? = project.description,
    javaVersion: JavaVersion? = null,
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
) {
    this.group = group
    this.version = version
    this.description = description
    javaVersion?.let(::setJavaVersion)
    textEncoding?.let(::setTextEncoding)
    if (hasShadowPlugin()) {
        archiveClassifier?.let(::setShadowArchiveClassifier)
        addBuildShadowTask()
    }
}

/**
 * Sets up the project with the specified [group] and [version] for a simple Minecraft project
 *
 * Calls [setupJava] and [addReplacementsTask] with the specified parameters
 *
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param replacements The replacements for the [replacements task][addReplacementsTask]
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 * @param archiveClassifier The archive classifier for the [shadow jar task][setShadowArchiveClassifier]
 */
@Ignore
fun Project.setupMC(
    group: String = project.group.toString(),
    version: String = project.version.toString(),
    description: String? = project.description,
    javaVersion: JavaVersion? = null,
    replacementFiles: Set<String>? = setOf("plugin.yml"),
    replacements: Map<String, String>? = mapOf("defaultReplacements" to "true"),
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
) {
    setupJava(group, version, description, javaVersion, textEncoding, archiveClassifier)
    if (replacementFiles != null && replacements != null) addReplacementsTask(replacementFiles, replacements)
}

/**
 * Sets up the project using Annoying API. **The [root project's name][Project.getName] must be the same as the one in plugin.yml**
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupMC] with the specified parameters
 * 3. Calls and returns [annoyingAPI] with the specified parameters
 *
 * @param annoyingAPIVersion The version of Annoying API to use (example: `3.0.1`)
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param replacements The replacements for the [replacements task][addReplacementsTask]
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 * @param archiveClassifier The archive classifier for the [shadow jar task][setShadowArchiveClassifier]
 * @param configurationAction The configuration for the Annoying API dependency
 */
@Ignore
fun Project.setupAnnoyingAPI(
    annoyingAPIVersion: String,
    group: String = project.group.toString(),
    version: String = project.version.toString(),
    description: String? = project.description,
    javaVersion: JavaVersion? = null,
    replacementFiles: Set<String>? = setOf("plugin.yml"),
    replacements: Map<String, String>? = mapOf("defaultReplacements" to "true"),
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
    configuration: String = "implementation",
    configurationAction: ExternalModuleDependency.() -> Unit = {},
): ExternalModuleDependency {
    check(hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }
    setupMC(group, version, description, javaVersion, replacementFiles, replacements, textEncoding, archiveClassifier)
    return annoyingAPI(annoyingAPIVersion, configuration, configurationAction)
}

/**
 * Sets up the project using JDA (Java-Discord API)
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupJava] with the specified parameters
 * 3. Calls [setMainClass] with the specified [mainClassName]
 * 4. Adds the `-parameters` compiler argument using [addCompilerArgs]
 * 5. Calls and returns [jda] with the specified [jdaVersion]
 *
 * @param jdaVersion The version of JDA to use (example: `5.1.0`)
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param mainClassName The main class name of the project (example: `xyz.srnyx.lazylibrary.LazyLibrary`)
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 * @param archiveClassifier The archive classifier for the [shadow jar task][setShadowArchiveClassifier]
 *
 * @return The JDA dependency that was created
 */
fun Project.setupJda(
    jdaVersion: String,
    excludeOpus: Boolean = true,
    group: String = project.group.toString(),
    version: String = project.version.toString(),
    description: String? = project.description,
    javaVersion: JavaVersion? = null,
    mainClassName: String? = null,
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
): ExternalModuleDependency {
    check(hasShadowPlugin()) { "Shadow plugin is required for JDA!" }
    setupJava(group, version, description, javaVersion, textEncoding, archiveClassifier)
    setMainClass(mainClassName)
    addCompilerArgs("-parameters")

    // Fix some tasks
    tasks["distZip"].dependsOn("shadowJar")
    tasks["distTar"].dependsOn("shadowJar")
    tasks["startScripts"].dependsOn("shadowJar")
    tasks["startShadowScripts"].dependsOn("jar")

    return jda(jdaVersion) {
        if (excludeOpus) exclude(module = "opus-java")
    }
}

/**
 * Sets up the project using Lazy Library
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupJda] with the specified parameters
 * 3. Adds `compileOnly` dependencies for documentation
 * 4. Fixes some tasks
 * 5. Calls and returns [lazyLibrary] with the specified [lazyLibraryVersion]
 *
 * @param lazyLibraryVersion The version of Lazy Library to use (example: `3.1.0`)
 * @param jdaVersion The version of JDA to use (example: `5.1.0`)
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param mainClassName The main class name of the project (example: `xyz.srnyx.lazylibrary.LazyLibrary`)
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 * @param archiveClassifier The archive classifier for the [shadow jar task][setShadowArchiveClassifier]
 *
 * @return The Lazy Library dependency that was created
 */
@Ignore
fun Project.setupLazyLibrary(
    lazyLibraryVersion: String,
    jdaVersion: String,
    excludeOpus: Boolean = true,
    group: String = project.group.toString(),
    version: String = project.version.toString(),
    description: String? = project.description,
    javaVersion: JavaVersion? = null,
    mainClassName: String? = null,
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
): ExternalModuleDependency {
    check(hasShadowPlugin()) { "Shadow plugin is required for Lazy Library!" }
    setupJda(jdaVersion, excludeOpus, group, version, description, javaVersion, mainClassName, textEncoding, archiveClassifier)

    // Add compileOnly dependencies for documentation
    dependencies.add("compileOnly", "io.github.freya022:BotCommands:2.10.4")
    dependencies.add("compileOnly", "org.spongepowered:configurate-yaml:4.1.2")

    return lazyLibrary(lazyLibraryVersion)
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