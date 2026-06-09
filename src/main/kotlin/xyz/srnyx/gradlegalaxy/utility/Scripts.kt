package xyz.srnyx.gradlegalaxy.utility

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository

import kotlin.text.replace


/**
 * Makes the given package path safe to use
 * - Converts the path to lowercase
 * - Removes all characters that are **not** `a-z`, `0-9`, `.`, or `_`
 *
 * @param path The package path to make safe
 *
 * @return The safe package path
 */
fun makePackageSafe(path: String): String = path.lowercase().replace("[^a-z0-9._]".toRegex(), "")

/**
 * Gets the main package of the project
 */
fun Project.getPackage(): String = "$group.${makePackageSafe(name)}"

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
fun Project.hasShadowPlugin(): Boolean = try {
    plugins.hasPlugin(ShadowPlugin::class.java)
} catch (_: NoClassDefFoundError) {
    false
}

/**
 * Checks if the `maven-publish` plugin is applied
 *
 * @return If the `maven-publish` plugin is applied
 */
fun Project.hasPublishPlugin(): Boolean = plugins.hasPlugin("maven-publish")

/**
 * Checks if the `application` plugin is applied
 *
 * @return If the `application` plugin is applied
 */
fun Project.hasApplicationPlugin(): Boolean = plugins.hasPlugin("application")

/**
 * Gets the Java plugin extension
 *
 * @return The Java plugin extension
 */
fun Project.getJavaExtension(): JavaPluginExtension {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    return extensions["java"] as JavaPluginExtension
}

fun Project.getPublishing(): PublishingExtension {
    check(hasPublishPlugin()) { "Publish plugin is not applied!" }
    return extensions["publishing"] as PublishingExtension
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
    "mainPackage" to getPackage(),
)

/**
 * Gets the environment variable with the specified name
 *
 * @param name The name of the environment variable
 *
 * @return The value of the environment variable, or `null` if it is not set or is blank
 */
fun getEnvironmentVariable(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

/**
 * Sets the text encoding for the project
 *
 * @param encoding The encoding to set
 */
fun Project.setTextEncoding(encoding: String = "UTF-8") {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    tasks.withType<JavaCompile> { options.encoding = encoding }
}

/**
 * Sets the Java version for the project
 *
 * @param version The java version to set (example: [JavaVersion.VERSION_1_8])
 */
fun Project.setJavaVersion(version: JavaVersion = JavaVersion.VERSION_1_8) {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
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
fun Project.addReplacementsTask(files: Set<String> = setOf("plugin.yml"), replacements: Map<String, String> = getDefaultReplacements()) {
    tasks.named<Copy>("processResources") {
        outputs.upToDateWhen { false }
        filesMatching(files) {
            expand(if (replacements["defaultReplacements"] == "true") getDefaultReplacements() + replacements.minus("defaultReplacements") else replacements)
        }
    }
}

/**
 * Adds the specified compiler arguments to the project
 *
 * @param args The compiler arguments to add
 */
fun Project.addCompilerArgs(vararg args: String) {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    tasks.withType<JavaCompile> { options.compilerArgs.addAll(args) }
}

/**
 * Sets the main class for the project
 *
 * @param mainClassName The main class name to set, uses "[getPackage].[Project.getName]" if null
 */
fun Project.setMainClass(mainClassName: String? = null) {
    check(hasApplicationPlugin()) { "Application plugin is not applied!" }
    extensions.configure<JavaApplication>("application") { mainClass.set(mainClassName ?: "${getPackage()}.${project.name}") }
}

/**
 * Relocates the specified package to the specified package
 *
 * @param from The package to relocate
 * @param to The package to relocate to
 */
fun Project.relocate(
    from: String,
    to: String = "${getPackage()}.libs.${makePackageSafe(from.split(".").last())}",
    action: SimpleRelocator.() -> Unit = {},
) {
    check(hasShadowPlugin()) { "Shadow plugin is not applied!" }
    tasks.named<ShadowJar>("shadowJar") { relocate(from, to, action) }
}

/**
 * Gets the metadata for the Annoying API with the specified version
 *
 * @param version The version of the Annoying API to get metadata for
 *
 * @return The metadata for the Annoying API
 */
fun Project.getAnnoyingApiMetadata(version: String): AnnoyingMetadata? {
    // Add srnyx's repositories
    repository(Repository.SRNYX_RELEASES, Repository.SRNYX_SNAPSHOTS)

    // Get JAR
    val file = runCatching {
        val metadataConfig = configurations.detachedConfiguration(dependencies.create("xyz.srnyx:annoying-api:$version:metadata@json"))
        metadataConfig.resolve().firstOrNull()
    }.getOrNull() ?: return null

    // Get text
    val text = file.readText()

    // Decode metadata
    return Json.decodeFromString<AnnoyingMetadata>(text)
}
