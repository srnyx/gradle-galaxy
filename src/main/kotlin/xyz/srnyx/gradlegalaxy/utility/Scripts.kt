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
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.GenerateRuntimeLibraryEnumConfig
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.RuntimeLibrariesConfig
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import kotlin.apply

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
 * Replaces all dots in the string with `{}`
 */
fun String.dotsToBrackets(): String = replace(".", "{}")

/**
 * Removes `{package}.libs.` and runs [dotsToBrackets]
 */
fun String.processRelocationTo(): String = replace("{package}.libs.", "").dotsToBrackets()

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

private val json = Json {
    ignoreUnknownKeys = true
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
    return json.decodeFromString<AnnoyingMetadata>(text)
}

/**
 * Processes the specified runtime libraries
 * 1. Adds the specified repositories
 * 2. Adds the specified dependencies (with exclusions)
 * 3. Relocates the specified libraries to the project's package
 *
 * @param libraries The runtime libraries to process
 * @param addRepositories Whether to add the repositories
 * @param configurations The configurations to add the dependencies to
 * @param relocate Whether to relocate the libraries
 */
fun Project.processRuntimeLibraries(
    libraries: Collection<RuntimeLibrary>,
    runtimeLibrariesConfig: RuntimeLibrariesConfig = RuntimeLibrariesConfig(),
) {
    val getPackage = getPackage()
    libraries.forEach { library ->
        // Add repositories
        if (runtimeLibrariesConfig.addRepositories) library.repositories.forEach { repo -> repository(repo) }

        // Add dependencies
        runtimeLibrariesConfig.configurations.forEach { configuration ->
            dependencies.add(configuration, "${library.group}:${library.artifact}:${library.version}") {
                // Excludes
                library.excludes.forEach { exclude(it.group, it.module) }
            }
        }

        // Relocations
        if (runtimeLibrariesConfig.relocate) library.relocations.forEach { relocation ->
            val to = relocation.to?.replace("{package}", getPackage)
            if (to != null) {
                relocate(relocation.from, to)
            } else {
                relocate(relocation.from)
            }
        }
    }
}

fun Project.generateAnnoyingApiRuntimeLibraryEnum(
    libraries: Collection<RuntimeLibrary>,
    generateRuntimeLibraryEnumConfig: GenerateRuntimeLibraryEnumConfig = GenerateRuntimeLibraryEnumConfig(),
    annoyingMetadata: AnnoyingMetadata? = null,
) {
    val packagePath = generateRuntimeLibraryEnumConfig.packagePath ?: getPackage()
    val packageFolder = packagePath.replace(".", "/")
    val enumName = "${name}Library"
    val annoyingPackage = annoyingMetadata?.packageName ?: "xyz.srnyx.annoyingapi"

    val enum = buildString {
        // Package
        append("package $packagePath.library;")
        append("\n")

        // Imports
        val libsLibby = "${if (generateRuntimeLibraryEnumConfig.relocateImports) "$annoyingPackage.libs" else "net.byteflux"}.libby"
        append("\nimport $libsLibby.Library;")
        append("\nimport $libsLibby.Repositories;")
        append("\nimport $libsLibby.relocation.Relocation;")
        append("\nimport org.jetbrains.annotations.NotNull;")
        append("\nimport org.jetbrains.annotations.Nullable;")
        append("\nimport xyz.srnyx.annoyingapi.AnnoyingPlugin;")
        append("\nimport xyz.srnyx.annoyingapi.BuildProperties;")
        append("\nimport xyz.srnyx.annoyingapi.library.AnnoyingLibrary;")
        append("\n")
        append("\nimport java.util.Arrays;")
        append("\nimport java.util.Collection;")
        append("\nimport java.util.Collections;")
        append("\nimport java.util.List;")
        append("\nimport java.util.function.Function;")
        append("\nimport java.util.function.Supplier;")
        append("\n")
        append("\n")

        // Enum declaration
        append("\npublic enum $enumName implements AnnoyingLibrary {")
        append("\n")

        // Libraries
        libraries.forEachIndexed { index, library ->
            append(buildLibraryEntry(library))
            if (index < libraries.size - 1) append(",\n")
        }
        append(";\n")
        append("\n")

        // Enum variables/constructors/methods
        append("""
        @NotNull public final Supplier<Library.Builder> librarySupplier;
        @Nullable public final Function<AnnoyingPlugin, Collection<Relocation>> relocations;
        @Nullable public final Collection<AnnoyingLibrary> requiredLibraries;
    
        $enumName(@NotNull Supplier<Library.Builder> librarySupplier) {
            this(librarySupplier, null, null);
        }
    
        $enumName(@NotNull Supplier<Library.Builder> librarySupplier, @NotNull Function<AnnoyingPlugin, Collection<Relocation>> relocations) {
            this(librarySupplier, relocations, null);
        }
    
        $enumName(@NotNull Supplier<Library.Builder> librarySupplier, @NotNull Collection<AnnoyingLibrary> requiredLibraries) {
            this(librarySupplier, null, requiredLibraries);
        }
    
        $enumName(@NotNull Supplier<Library.Builder> librarySupplier, @Nullable Function<AnnoyingPlugin, Collection<Relocation>> relocations, @Nullable Collection<AnnoyingLibrary> requiredLibraries) {
            this.librarySupplier = librarySupplier;
            this.relocations = relocations;
            this.requiredLibraries = requiredLibraries;
        }
    
        @Override @NotNull
        public Supplier<Library.Builder> getLibrarySupplier() {
            return librarySupplier;
        }
    
        @Override @Nullable
        public Function<AnnoyingPlugin, Collection<Relocation>> getRelocations() {
            return relocations;
        }
    
        @Override @Nullable
        public Collection<AnnoyingLibrary> getRequiredLibraries() {
            return requiredLibraries;
        }
    }
    """.trimIndent())
    }

    // Register task to generate Enum file
    val outputDir = project.layout.buildDirectory.dir("generated/sources/gradle-galaxy/main/java")
    val outputFile = outputDir.map { it.file("$packageFolder/library/$enumName.java") }
    val generateEnumTask = project.tasks.register("generateRuntimeLibrary") {
        group = "build"
        description = "Generates the $enumName enum for the Annoying API runtime libraries"

        inputs.property("enum", enum)
        outputs.dir(outputDir)

        doLast {
            outputFile.get().asFile.apply {
                parentFile.mkdirs()
                writeText(enum)
            }
        }
    }

    // Wire generated directory into main Java source set
    project.extensions.configure<JavaPluginExtension> {
        sourceSets.named("main") {
            java.srcDir(generateEnumTask.map { it.outputs.files })
        }
    }
}

private fun buildLibraryEntry(library: RuntimeLibrary): String = buildString {
    append("    ${library.name.uppercase()}(")
    append("\n            () -> Library.builder()")

    // Repositories
    library.repositories.forEach { repository ->
        append("\n                    .repository(\"$repository\")")
    }

    // Core properties
    append("\n                    .groupId(\"${library.group.dotsToBrackets()}\")")
    append("\n                    .artifactId(\"${library.artifact}\")")
    append("\n                    .version(\"${library.version}\")")

    // Relocations
    if (library.relocations.isNotEmpty()) {
        append(",\n            plugin -> List.of(")
        library.relocations.forEachIndexed { i, relocation ->
            append("\n                    plugin.getRelocation(\"${relocation.from.dotsToBrackets()}\"")
            relocation.to?.let { append(", \"${it.processRelocationTo()}\"") }
            append(")")
            if (i < library.relocations.size - 1) append(",")
        }
        append(")")
    }

    // Dependencies
    if (library.dependencies.isNotEmpty()) {
        append(",\n            List.of(")
        library.dependencies.forEachIndexed { i, dependency ->
            append("\n                    ${dependency.uppercase()}")
            if (i < library.dependencies.size - 1) append(",")
        }
        append(")")
    }

    append(")")
}
