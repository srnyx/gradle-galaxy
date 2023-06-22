package xyz.srnyx.gradlegalaxy.utility

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Action

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.data.pom.LicenseData
import xyz.srnyx.gradlegalaxy.data.pom.ScmData


/**
 * Returns a map that tells [addReplacementsTask] to use the default replacements
 *
 * @return The map that tells [addReplacementsTask] to use the default replacements
 *
 * @see addReplacementsTask
 */
fun getSentinelReplacements(): Map<String, String> = mapOf("defaultReplacements" to "true")

/**
 * Checks if the `java` plugin is applied
 *
 * @return If the `java` plugin is applied
 */
fun Project.hasJavaPlugin(): Boolean = plugins.hasPlugin("java")

/**
 * Checks if the Shadow plugin is applied
 *
 * @return If the Shadow plugin is applied
 */
fun Project.hasShadowPlugin(): Boolean = plugins.hasPlugin("com.github.johnrengelman.shadow")

/**
 * Gets the Java plugin extension
 *
 * @return The Java plugin extension
 */
fun Project.getJavaExtension(): JavaPluginExtension {
    return extensions["java"] as JavaPluginExtension
}

/**
 * Returns the default replacements map for [addReplacementsTask]
 *
 * @return The default replacements map
 *
 * @see addReplacementsTask
 */
fun Project.getDefaultReplacements(): Map<String, String> = mapOf(
    "group" to group.toString(),
    "name" to name,
    "version" to version.toString(),
    "description" to description.toString(),
)

/**
 * Sets the text encoding for the project
 *
 * @param encoding The encoding to set
 */
fun Project.setTextEncoding(encoding: String = "UTF-8") {
    tasks.withType<JavaCompile> { options.encoding = encoding }
}

/**
 * Sets the Java version for the project
 *
 * @param version The java version to set (example: [JavaVersion.VERSION_1_8])
 */
fun Project.setJavaVersion(version: JavaVersion = JavaVersion.VERSION_1_8) {
    val java: JavaPluginExtension = getJavaExtension()
    java.sourceCompatibility = version
    java.targetCompatibility = version
}

/**
 * Sets the artifact/archive classifier for the JAR and Shadow JAR tasks
 *
 * @param classifier The classifier to set
 */
fun Project.setShadowArchiveClassifier(classifier: String = "") {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    tasks.named<ShadowJar>("shadowJar") { archiveClassifier.set(classifier) }
}

/**
 * Adds the task that makes `gradle build` run `gradle shadowJar`
 */
fun Project.addBuildShadowTask() {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    tasks.named<DefaultTask>("build") { dependsOn("shadowJar") }
}

/**
 * Adds the task that generates the Javadoc and sources jar files
 *
 * @param javadocClassifier The classifier for the Javadoc jar file
 * @param sourcesClassifier The classifier for the sources jar file
 */
@Ignore
fun Project.addJavadocSourcesJars(javadocClassifier: String? = null, sourcesClassifier: String? = null) {
    val java: JavaPluginExtension = getJavaExtension()
    java.withJavadocJar()
    java.withSourcesJar()
    javadocClassifier?.let { tasks.named<Jar>("javadocJar") { archiveClassifier.set(it) } }
    sourcesClassifier?.let { tasks.named<Jar>("sourcesJar") { archiveClassifier.set(it) } }
}

/**
 * Configures the ProcessResources task to add replacements
 *
 * @param replacements A [Map] of all the replacements
 */
fun Project.addReplacementsTask(replacements: Map<String, String> = getDefaultReplacements()) {
    tasks.named<Copy>("processResources") {
        outputs.upToDateWhen { false }
        filesMatching("**/*.yml") {
            expand(if (replacements == getSentinelReplacements()) getDefaultReplacements() else replacements)
        }
    }
}

/**
 * Adds the specified compiler arguments to the project
 *
 * @param args The compiler arguments to add
 */
@Ignore
fun Project.addCompilerArgs(vararg args: String) {
    tasks.withType<JavaCompile> { options.compilerArgs.addAll(args) }
}

/**
 * Relocates the specified package to the specified package
 *
 * @param from The package to relocate
 * @param to The package to relocate to
 */
@Ignore
fun Project.relocate(
    from: String,
    to: String = "${project.group}.${project.name.lowercase().filter { char -> char.isLetterOrDigit() || char in "._" }}.libs.${from.split(".").last()}",
) {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    tasks.named<ShadowJar>("shadowJar") { relocate(from, to) }
}

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
    replacements: Map<String, String>? = getSentinelReplacements(),
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
) {
    setupJava(group, version, description, javaVersion, textEncoding, archiveClassifier)
    replacements?.let(::addReplacementsTask)
}

/**
 * Sets up the project using Annoying API
 *
 * 1. Checks if the Shadow plugin is applied
 * 2. Calls [setupMC] with the specified parameters
 * 3. Calls and returns [annoyingAPI] with the specified parameters
 *
 * @param annoyingAPIVersion The version of Annoying API to use (example: `3.0.1`)
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
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
    replacements: Map<String, String>? = getSentinelReplacements(),
    textEncoding: String? = "UTF-8",
    archiveClassifier: String? = "",
    configuration: String = "implementation",
    configurationAction: Action<ExternalModuleDependency> = Action {}
): ExternalModuleDependency {
    check(hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }
    setupMC(group, version, description, javaVersion, replacements, textEncoding, archiveClassifier)
    return annoyingAPI(annoyingAPIVersion, configuration, configurationAction)
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
