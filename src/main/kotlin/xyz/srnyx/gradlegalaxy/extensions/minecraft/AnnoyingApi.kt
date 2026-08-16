package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.annoyingapi.Exclude
import xyz.srnyx.gradlegalaxy.data.annoyingapi.Relocation
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import xyz.srnyx.gradlegalaxy.extensions.JavaExtension
import xyz.srnyx.gradlegalaxy.utility.dotsToBrackets
import xyz.srnyx.gradlegalaxy.utility.getAnnoyingApiMetadata
import xyz.srnyx.gradlegalaxy.utility.getPackage
import xyz.srnyx.gradlegalaxy.utility.hasJavaPlugin
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.processRelocationTo
import xyz.srnyx.gradlegalaxy.utility.relocate
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import javax.inject.Inject


/**
 * `minecraft { annoyingAPI(version) { } }` alone already applies full [java]/[minecraft]/`minecraft.runPaper`
 * defaults — no separate `galaxy { java { } }` / `minecraft { }` / `minecraft { runPaper { } }` calls needed.
 */
abstract class AnnoyingApiExtension @Inject constructor(
    objects: ObjectFactory,
    private val java: JavaExtension,
    private val minecraft: MinecraftExtension,
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.SRNYX_SNAPSHOTS, REPOSITORIES.SRNYX_RELEASES))
        group.set("xyz.srnyx")
        name.set("annoying-api")
        configurations.set(listOf("implementation", "testImplementation"))
    } }
    val metadata = objects.newInstance(MetadataExtension::class.java)
    val customRuntimeLibraries = objects.newInstance(CustomRuntimeLibrariesExtension::class.java)

    fun metadata(action: MetadataExtension.() -> Unit) = metadata.action()
    fun customRuntimeLibraries(action: CustomRuntimeLibrariesExtension.() -> Unit) = customRuntimeLibraries.action()

    internal fun setup(project: Project) {
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }
        check(project.hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }

        // Every Annoying API project needs these — applied unconditionally. Each has its own
        // idempotency guard, so this is a no-op wherever the consumer already triggered them
        // themselves (e.g. a separate top-level `galaxy { java { } }`).
        minecraft.setup(project)
        minecraft.runPaper.setup(project)

        val annoyingMetadata: AnnoyingMetadata? = metadata.process(project, this)

        customRuntimeLibraries.process(project, annoyingMetadata)
    }
}

abstract class MetadataExtension @Inject constructor(
    objects: ObjectFactory,
) {
    @get:Input
    val useMetadata: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val relocateAnnoyingAPI: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val addRepositories: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val excludes: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    var runtimeLibraries = objects.newInstance(RuntimeLibrariesExtension::class.java)

    fun runtimeLibraries(action: RuntimeLibrariesExtension.() -> Unit) = runtimeLibraries.action()

    internal fun process(
        project: Project,
        annoyingApiDependency: DependencyExtension,
    ): AnnoyingMetadata? {
        // Get and process Annoying API metadata
        val metadata = useMetadata.takeIf { it.get() }?.let { project.getAnnoyingApiMetadata(annoyingApiDependency.version.get()) }
        if (metadata != null) {
            // Relocate Annoying API
            if (relocateAnnoyingAPI.get()) project.relocate(metadata.packageName)

            // Java version (only if custom not specified)
            if (setJavaVersion.get() && metadata.javaVersion != null) {
                project.setJavaVersion(JavaVersion.toVersion(metadata.javaVersion))
            }

            // Repositories
            if (addRepositories.get()) metadata.repositories.forEach { project.repositories.maven(it) }

            // Runtime libraries
            runtimeLibraries.libraries.set(metadata.runtimeLibraries)
            runtimeLibraries.process(project)
        }

        // Excludes
        if (excludes.get()) {
            val original = annoyingApiDependency.action
            annoyingApiDependency.action = {
                metadata?.excludes?.forEach { exclude(it.group, it.module) }
                original()
            }
        }

        return metadata
    }
}

abstract class CustomRuntimeLibrariesExtension @Inject constructor(
    private val objects: ObjectFactory
) : RuntimeLibrariesExtension(objects) {
    init {
        configurations.convention(listOf("compileOnly", "testImplementation"))
    }

    var generateRuntimeLibraryEnum: GenerateRuntimeLibraryEnumExtension = objects.newInstance(GenerateRuntimeLibraryEnumExtension::class.java)

    @Used
    fun library(name: String, action: RuntimeLibraryBuilder.() -> Unit) {
        val builder = RuntimeLibraryBuilder(objects, name)
        builder.action()
        libraries.add(builder.build())
        libraries.addAll(builder.children)
    }
    fun generateRuntimeLibraryEnum(action: GenerateRuntimeLibraryEnumExtension.() -> Unit) = generateRuntimeLibraryEnum.action()

    /** Backward-compat bridge for the deprecated `CustomRuntimeLibrariesConfig(runtimeLibraries = listOf(...))`. */
    internal fun addRawLibraries(libraries: List<RuntimeLibrary>) {
        this.libraries.addAll(libraries)
    }

    internal fun process(
        project: Project,
        annoyingMetadata: AnnoyingMetadata?
    ) {
        if (libraries.orNull.isNullOrEmpty()) return

        // Process libraries
        super.process(project)

        // Generate enum
        generateRuntimeLibraryEnum.process(project, libraries.get(), annoyingMetadata)
    }
}

class RuntimeLibraryBuilder internal constructor(
    private val objects: ObjectFactory,
    private val name: String,
) {
    /** Nested [library] declarations, flattened in parent-before-descendant (pre-order) declaration order */
    internal val children: MutableList<RuntimeLibrary> = mutableListOf()

    @get:Input
    val group: Property<String> = objects.property(String::class.java)
    @get:Input
    val artifact: Property<String> = objects.property(String::class.java)
    @get:Input
    val version: Property<String> = objects.property(String::class.java)
    /**
     * Names of other declared runtime libraries that this one depends on
     */
    @get:Input
    val dependencies: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    @get:Input
    val repositories: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    @get:Input
    val excludes: ListProperty<Exclude> = objects.listProperty(Exclude::class.java).convention(emptyList())
    @get:Input
    val relocations: ListProperty<Relocation> = objects.listProperty(Relocation::class.java).convention(emptyList())


    @Used
    fun exclude(group: String, module: String) {
        excludes.add(Exclude(group, module))
    }

    /** Relocates [from] to `{package}.libs.<lastSegmentOf(from)>` if [to] isn't specified. */
    @Used
    fun relocate(from: String, to: String? = null) {
        relocations.add(Relocation(from, to))
    }

    /**
     * Declares a runtime library named [name] nested under this one — inherits [repositories], [group],
     * [artifact], [version], [excludes], and [relocations] from this (enclosing) library as conventions
     * (anything set explicitly on the nested builder still wins), and adds this library as a [dependencies]
     * entry unless [dependency] is `false`.
     */
    @Used
    fun library(name: String, dependency: Boolean = true, action: RuntimeLibraryBuilder.() -> Unit = {}) {
        val builder = RuntimeLibraryBuilder(objects, name)
        builder.repositories.convention(repositories)
        builder.group.convention(group)
        builder.artifact.convention(artifact)
        builder.version.convention(version)
        builder.excludes.convention(excludes)
        builder.relocations.convention(relocations)
        if (dependency) builder.dependencies.add(this.name)
        builder.action()
        children.add(builder.build())
        children.addAll(builder.children)
    }

    internal fun build(): RuntimeLibrary = RuntimeLibrary(
        name = name,
        repositories = repositories.get(),
        group = group.get(),
        artifact = artifact.get(),
        version = version.get(),
        excludes = excludes.get(),
        relocations = relocations.get(),
        dependencies = dependencies.get()
    )
}

/** Processes a set of [RuntimeLibrary] dependencies: adds their repositories, dependencies, and relocations. */
abstract class RuntimeLibrariesExtension @Inject constructor(
    objects: ObjectFactory
) {
    @get:Input
    val libraries: ListProperty<RuntimeLibrary> = objects.listProperty(RuntimeLibrary::class.java)
    @get:Input
    val addRepositories: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    /**
     * Dependency classpaths to add the dependencies to (e.g. `compileOnly`, `implementation`, `testImplementation`, etc.).
     *
     * If empty, dependencies will not be added to any classpath.
     *
     * You usually don't need to change this as dependencies are on the `compileOnlyApi` classpath on Annoying API.
     */
    @get:Input
    val configurations: ListProperty<String> = objects.listProperty(String::class.java).convention(listOf("testImplementation"))
    @get:Input
    val relocate: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    internal fun process(project: Project) {
        if (libraries.orNull.isNullOrEmpty()) return

        val getPackage = project.getPackage()
        libraries.get().forEach { library ->
            // Add repositories
            if (addRepositories.get()) library.repositories.forEach { repo -> project.repositories.maven(repo) }

            // Add dependencies
            configurations.get().forEach { configuration ->
                project.dependencies.add(configuration, "${library.group}:${library.artifact}:${library.version}") {
                    // Excludes
                    library.excludes.forEach { exclude(it.group, it.module) }
                }
            }

            // Relocations
            if (relocate.get()) library.relocations.forEach { relocation ->
                val to = relocation.to?.replace("{package}", getPackage)
                if (to != null) {
                    project.relocate(relocation.from, to)
                } else {
                    project.relocate(relocation.from)
                }
            }
        }
    }
}

interface GenerateRuntimeLibraryEnumExtension {
    @get:Input @get:Optional
    val relocateImports: Property<Boolean>
    @get:Input @get:Optional
    val packagePath: Property<String>

    fun process(
        project: Project,
        libraries: List<RuntimeLibrary>,
        annoyingMetadata: AnnoyingMetadata? = null,
    ) {
        val packagePath = packagePath.getOrElse(project.getPackage())
        val packageFolder = packagePath.replace(".", "/")
        val enumName = "${project.name}Library"

        val enum = buildString {
            // Package
            append("package $packagePath.library;")
            append("\n")

            // Imports
            val annoyingPackage = annoyingMetadata?.packageName ?: "xyz.srnyx.annoyingapi"
            val libsLibby = "${if (relocateImports.getOrElse(true)) "$annoyingPackage.libs" else "net.byteflux"}.libby"
            append("\nimport $libsLibby.Library;")
            append("\nimport $libsLibby.relocation.Relocation;")
            append("\nimport org.jetbrains.annotations.NotNull;")
            append("\nimport org.jetbrains.annotations.Nullable;")
            append("\nimport $annoyingPackage.AnnoyingPlugin;")
            append("\nimport $annoyingPackage.library.AnnoyingLibrary;")
            append("\n")
            append("\nimport java.util.Collection;")
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
            group = "galaxy"
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
}
