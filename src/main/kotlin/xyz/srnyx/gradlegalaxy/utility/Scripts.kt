package xyz.srnyx.gradlegalaxy.utility

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.*
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.GenerateRuntimeLibraryEnumConfig
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.extensions.GradleGalaxyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import kotlin.text.replace


private val json by lazy { Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
} }

/**
 * @return  Whether the project is running in a GitHub Actions workflow
 */
val Project.inGitHubWorkflow: Boolean
    get() = project.getEnvironmentVariable("GITHUB_WORKFLOW") != null

/**
 * @return  Whether the project is running in a GitHub Actions publish (release/pre-release) workflow
 */
val Project.inGitHubPublish: Boolean
    get() = project.getEnvironmentVariable("GITHUB_REF_TYPE") == "tag"

/**
 * @return  Whether the project is running in a GitHub Actions pre-release workflow
 */
val Project.inGitHubPreRelease: Boolean
    get() {
        if (!inGitHubPublish) return false
        val eventPath = project.getEnvironmentVariable("GITHUB_EVENT_PATH") ?: return false
        return json.decodeFromString<JsonObject>(File(eventPath).readText())["release"]?.jsonObject
            ?.get("prerelease")?.jsonPrimitive
            ?.booleanOrNull ?: false
    }

/**
 * @return  Whether the project is running in a GitHub Actions release workflow
 */
@Used
val Project.inGitHubRelease: Boolean
    get() = inGitHubPublish && !inGitHubPreRelease

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
 * @return  If the `me.modmuss50.mod-publish-plugin` plugin is applied
 */
fun Project.hasModPublishPlugin(): Boolean = plugins.hasPlugin("me.modmuss50.mod-publish-plugin")

/**
 * @return  If the `io.papermc.hangar-publish-plugin` plugin is applied
 */
fun Project.hasHangarPublishPlugin(): Boolean = plugins.hasPlugin("io.papermc.hangar-publish-plugin")

/**
 * @return  If the `xyz.jpenilla.run-paper` plugin is applied
 */
fun Project.hasRunPaperPlugin(): Boolean = plugins.hasPlugin("xyz.jpenilla.run-paper")

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

private var dotEnv: MutableMap<String, String>? = null

/**
 * Loads environment variables from the `.env` file
 *
 * Returns cached map if already loaded
 */
fun Project.dotEnv(): Map<String, String> {
    if (dotEnv != null) return dotEnv!!
    dotEnv = mutableMapOf()

    val file = layout.projectDirectory.file(".env").asFile
    if (file.exists()) file.forEachLine { line ->
        // Skip comments and empty lines
        if (line.isNotBlank() && !line.startsWith("#") && line.contains("=")) {
            val parts = line.split("=", limit = 2)
            val key = parts[0].trim()
            val value = parts[1].trim().removeSurrounding("\"") // Remove quotes

            if (value.isNotBlank()) dotEnv?.set(key, value)
        }
    }
    return dotEnv!!
}

/**
 * Gets the environment variable with the specified name
 *
 * @param name The name of the environment variable
 *
 * @return The value of the environment variable, or `null` if it is not set or is blank
 */
fun Project.getEnvironmentVariable(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
    ?: run { dotEnv()[name]?.takeIf { it.isNotBlank() } }

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
 * @param force Whether to set the version even if the user already set an explicit `galaxy { java { javaVersion = ... } } }`
 */
fun Project.setJavaVersion(version: JavaVersion = JavaVersion.VERSION_1_8, force: Boolean = false) {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }

    // Let an explicit `galaxy { java { javaVersion = ... } }` always win over version defaults
    // inferred elsewhere (Paper/Spigot Minecraft-version detection, Annoying API metadata, etc.)
    if (!force && extensions.findByType(GradleGalaxyExtension::class.java)?.java?.javaVersion?.isPresent == true) return

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
 * Configures the `processResources` and `processTestResources` tasks to add replacements
 *
 * @param   files           The files to process replacements for
 * @param   replacements    A [Map] of all the replacements
 */
fun Project.addReplacementsTask(files: Set<String> = setOf("plugin.yml"), replacements: Map<String, String> = getDefaultReplacements()) {
    if (files.isEmpty() || replacements.isEmpty()) return

    val actualReplacements = if (replacements["defaultReplacements"] == "true") getDefaultReplacements() + replacements.minus("defaultReplacements") else replacements
    listOf("processResources", "processTestResources").forEach { taskName ->
        tasks.named<Copy>(taskName) {
            inputs.property("replacements", actualReplacements)

            filesMatching(files) {
                expand(actualReplacements)
            }
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
 * Silences missing JavaDoc warnings
 */
fun Project.silenceMissingJavaDocWarnings() {
    check(hasJavaPlugin()) { "Java plugin is not applied!" }
    tasks.withType<Javadoc> { (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet") }
}

/**
 * Replaces all dots in the string with `{}`
 */
fun String.dotsToBrackets(): String = replace(".", "{}")

/**
 * Removes `{package}.libs.` and runs [dotsToBrackets]
 */
fun String.processRelocationTo(): String = replace("{package}.libs.", "").dotsToBrackets()

/**
 * Retrieve the versions of the specified platform from Hangar
 *
 * @param platform The platform to retrieve versions for (default: `PAPER`)
 *
 * @return The versions of the specified platform in a [LinkedHashSet] sorted by version (highest to lowest)
 */
fun retrieveHangarPlatformVersions(platform: String = "PAPER"): LinkedHashSet<String> {
    // Make API request
    val response = HttpClient.newBuilder().build().send(
        HttpRequest.newBuilder()
            .uri(URI.create("https://hangar.papermc.io/api/v1/platforms/${platform}/versions"))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
        throw IllegalStateException("Failed to retrieve Hangar platform versions for $platform: ${response.statusCode()} ${response.body()}")
    }

    // Flatten versions
    val versions = LinkedHashSet<String>()
    for (element in json.decodeFromString<JsonArray>(response.body())) {
        val jsonObject = element.jsonObject
        // Add subVersions first as version is lowest
        for (subVersion in jsonObject["subVersions"]!!.jsonArray) versions.add(subVersion.jsonPrimitive.content)
        versions.add(jsonObject["version"]!!.jsonPrimitive.content)
    }
    return versions
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
 * Adds a task to generate the `platforms.json` resources file, listing out the plugin's publishing platforms
 *
 * @param platforms The platforms to add to the `platforms.json` file
 *
 * @return The task that generates the `platforms.json` resources file
 */
fun Project.addPlatformsResourceFileTask(platforms: Map<PluginPlatform, String>): TaskProvider<Task> {
    val platformsFile = project.layout.buildDirectory.file("resources/main/platforms.json").get().asFile
    val platformsProvider = project.provider { json.encodeToString(mapOf("platforms" to platforms)) }

    val task = tasks.register("writePlatformsResourceFile") {
        group = "build"
        description = "Writes the platforms.json file"

        inputs.property("text", platformsProvider)
        outputs.file(platformsFile)

        doLast {
            platformsFile.writeText(platformsProvider.get())
        }
    }

    project.tasks.named("processResources") { dependsOn(task) }

    return task
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
    repositories {
        maven(REPOSITORIES.SRNYX_RELEASES)
        maven(REPOSITORIES.SRNYX_SNAPSHOTS)
    }

    // Get JAR
    val file = runCatching {
        val metadataConfig = configurations.detachedConfiguration(dependencies.create("xyz.srnyx:annoying-api:$version:metadata@json"))
        metadataConfig.resolve().firstOrNull()
    }.getOrNull() ?: return null

    // Get text
    val text = file.readText()

    // Decode metadata
    return json.decodeFromString<AnnoyingMetadata>(text)
}

@Deprecated("Use galaxy { minecraft { annoyingAPI(version) { customRuntimeLibraries { ... } } } } instead")
fun Project.generateAnnoyingApiRuntimeLibraryEnum(
    libraries: Collection<RuntimeLibrary>,
    generateRuntimeLibraryEnumConfig: GenerateRuntimeLibraryEnumConfig = GenerateRuntimeLibraryEnumConfig(),
    annoyingMetadata: AnnoyingMetadata? = null, // Keep for backwards compatibility
) {
    extensions.configure<GradleGalaxyExtension>("galaxy") {
        minecraft {
            annoyingAPI.customRuntimeLibraries {
                addRawLibraries(libraries.toList())
                generateRuntimeLibraryEnum(generateRuntimeLibraryEnumConfig.toExtension())
            }
        }
    }
}
