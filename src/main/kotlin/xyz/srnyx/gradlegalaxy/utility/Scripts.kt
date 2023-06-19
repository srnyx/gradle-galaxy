package xyz.srnyx.gradlegalaxy.utility

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

import xyz.srnyx.gradlegalaxy.annotations.Ignore
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.ShadowVersion
import xyz.srnyx.gradlegalaxy.enums.mavenQuick


/**
 * Checks if the Shadow plugin is applied
 */
fun Project.hasShadowPlugin(): Boolean = plugins.hasPlugin("com.github.johnrengelman.shadow")

/**
 * Gets the Java plugin extension
 */
fun Project.getJavaExtension(): JavaPluginExtension {
    return extensions["java"] as JavaPluginExtension
}

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
 * @param javaVersion The java version to set (example: [JavaVersion.VERSION_1_8])
 */
fun Project.setJavaVersion(javaVersion: JavaVersion = JavaVersion.VERSION_1_8) {
    val java: JavaPluginExtension = getJavaExtension()
    java.sourceCompatibility = javaVersion
    java.targetCompatibility = javaVersion
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
 * Configures the ProcessResources [Task] to add replacements
 *
 * @param replacements A [Map] of all the replacements
 */
fun Project.addReplacementsTask(replacements: Map<String, () -> String> = mapOf(
    "name" to name::toString,
    "version" to version::toString
)) {
    tasks.named<Copy>("processResources") {
        outputs.upToDateWhen { false }
        filesMatching("**/*.yml") { expand(replacements) }
    }
}

/**
 * Adds the specified compiler arguments to the project
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
 * @param configuration The configuration of the [MavenPublication]
 *
 * @return The [MavenPublication] that was created
 */
@Ignore
inline fun Project.setupPublishing(
    groupId: String? = null,
    artifactId: String? = null,
    version: String? = null,
    component: SoftwareComponent? = components["java"],
    artifacts: Collection<Any> = emptyList(),
    crossinline configuration: MavenPublication.() -> Unit
): MavenPublication {
    apply(plugin = "maven-publish")
    return (extensions["publishing"] as PublishingExtension).publications.create<MavenPublication>("maven") {
        groupId?.let { this.groupId = it }
        artifactId?.let { this.artifactId = it }
        version?.let { this.version = it }
        component?.let { this.from(component) }
        artifacts.forEach(this::artifact)
        configuration()
    }
}

/**
 * Sets up the project with the specified [group] and [version] for a simple Minecraft project
 *
 * Adds the text encoding, replacements, and build shadow task (if the Shadow plugin is applied)
 *
 * @param group The group of the project (example: `me.dkim19375`)
 * @param version The version of the project (example: `1.0.0`)
 * @param dependency The dependency to add to the project (using `compileOnly`)
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param replacements The replacements for the [replacements task][addReplacementsTask]
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 */
@Ignore
fun Project.setupMC(
    group: String,
    version: String = "1.0.0",
    dependency: String? = null,
    javaVersion: JavaVersion? = null,
    replacements: Map<String, () -> String>? = mapOf(
        "name" to name::toString,
        "version" to version::toString
    ),
    textEncoding: String? = "UTF-8",
    artifactClassifier: String? = "",
) {
    apply(plugin = "java")
    this.group = group
    this.version = version
    dependency?.let { dependencies.add("compileOnly", it) }
    javaVersion?.let(::setJavaVersion)
    replacements?.let(::addReplacementsTask)
    textEncoding?.let(::setTextEncoding)
    if (hasShadowPlugin()) {
        artifactClassifier?.let(::setShadowArchiveClassifier)
        addBuildShadowTask()
    }
}

/**
 * Sets up the project using Annoying API
 *
 * 1. Applies the shadow plugin with the specified [shadowVersion]
 * 2. Calls [setupMC] with the specified parameters
 * 3. Adds the [Repository.JITPACK] repository
 * 4. Adds the Annoying API dependency (`implementation`)
 * 5. Relocates Annoying API using [relocate]
 */
@Ignore
fun Project.setupAnnoyingAPI(
    shadowVersion: String,
    annoyingAPIVersion: String,
    group: String,
    version: String = "1.0.0",
    dependency: String? = null,
    javaVersion: JavaVersion? = null,
    replacements: Map<String, () -> String>? = mapOf(
        "name" to name::toString,
        "version" to version::toString
    ),
    textEncoding: String? = "UTF-8",
    artifactClassifier: String? = "",
) {
    //TODO Apply the shadow plugin
    buildscript {
        repositories { gradlePluginPortal() }
        dependencies { add("classpath", "${ShadowVersion.parse(shadowVersion).groupId}:shadow:$shadowVersion") }
    }
    apply(plugin = "com.github.johnrengelman.shadow")

    // Everything else
    setupMC(group, version, dependency, javaVersion, replacements, textEncoding, artifactClassifier)
    repositories { mavenQuick(Repository.JITPACK) }
    dependencies { add("implementation", "xyz.srnyx:annoying-api:$annoyingAPIVersion") }
    relocate("xyz.srnyx.annoyingapi")
}
