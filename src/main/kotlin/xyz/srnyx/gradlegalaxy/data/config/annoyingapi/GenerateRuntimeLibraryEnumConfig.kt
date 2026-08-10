package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.configure
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.utility.dotsToBrackets
import xyz.srnyx.gradlegalaxy.utility.getPackage
import xyz.srnyx.gradlegalaxy.utility.processRelocationTo


data class GenerateRuntimeLibraryEnumConfig(
    val relocateImports: Boolean = true,
    val packagePath: String? = null,
) {
    internal fun toExtension(): GenerateRuntimeLibraryEnumExtension.() -> Unit = {
        val config: GenerateRuntimeLibraryEnumConfig = this@GenerateRuntimeLibraryEnumConfig

        relocateImports.set(config.relocateImports)
        packagePath.set(config.packagePath)
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
}
