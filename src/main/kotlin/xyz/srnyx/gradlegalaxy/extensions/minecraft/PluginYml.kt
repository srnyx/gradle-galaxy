package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.util.internal.VersionNumber
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.utility.getPackage
import java.io.File
import javax.inject.Inject


abstract class PluginYmlExtension @Inject constructor(
    objects: ObjectFactory
) {
    @Used val STARTUP = "STARTUP"
    @Used val POSTWORLD = "POSTWORLD"

    /**
     * Defaults to project name
     */
    @get:Input @get:Optional
    val name: Property<String> = objects.property(String::class.java)
    /**
     * Defaults to project version
     */
    @get:Input @get:Optional
    val version: Property<String> = objects.property(String::class.java)
    /**
     * Defaults to project description
     */
    @get:Input @get:Optional
    val description: Property<String> = objects.property(String::class.java)
    /**
     * Defaults to `mainPackage.name`
     */
    @get:Input @get:Optional
    val main: Property<String> = objects.property(String::class.java)
    /**
     * Defaults to Minecraft version from Paper/Spigot
     */
    @get:Input @get:Optional
    val apiVersion: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val authors: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val contributors: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val website: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val foliaSupported: Property<Boolean> = objects.property(Boolean::class.java)
    @get:Input @get:Optional
    val load: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val depend: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val softDepend: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val loadBefore: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val provides: ListProperty<String> = objects.listProperty(String::class.java)


    internal fun setup(
        project: Project,
        minecraft: MinecraftExtension,
    ) {
        if (name.orNull == null) name.set(project.name)
        if (version.orNull == null) version.set(project.version.toString())
        if (description.orNull == null) description.set(project.description)
        if (main.orNull == null) main.set("${project.getPackage()}.${project.name}")
        if (apiVersion.orNull == null) apiVersion.set(minecraft.getMinecraftVersion())
        if (foliaSupported.orNull == null) foliaSupported.set(minecraft.folia)

        // main
        val mainExtra = project.layout.projectDirectory.file("src/main/resources/plugin.yml")
        val mainText = project.provider { buildPluginYmlText(name.get(), main.get()) }
        project.tasks.named<ProcessResources>("processResources") {
            inputs.property("pluginYmlText", mainText)
            doLast { writePluginYml(destinationDir, mainText.get(), mainExtra) }
        }

        // test
        val testExtra = project.layout.projectDirectory.file("src/test/resources/plugin.yml")
        val mockName = project.provider { "Mock${name.get()}" }
        val mockMain = project.provider { main.get().let { "${it.substringBeforeLast('.')}.Mock${it.substringAfterLast('.')}" } }
        val testText = project.provider { buildPluginYmlText(mockName.get(), mockMain.get()) }
        project.tasks.named<ProcessResources>("processTestResources") {
            inputs.property("pluginYmlText", testText)
            doLast { writePluginYml(destinationDir, testText.get(), testExtra) }
        }
    }

    /**
     * Builds the `plugin.yml` text, using [nameValue]/[mainValue] instead of [name]/[main] so the `Mock`-prefixed test variant can reuse it
     */
    private fun buildPluginYmlText(nameValue: String, mainValue: String): String = buildString {
        appendLine("name: $nameValue")
        appendLine("version: ${version.get()}")
        appendLine("description: ${description.get()}")
        appendLine("main: $mainValue")
        apiVersion.orNull?.let { appendLine("api-version: ${normalizeApiVersion(it)}") }
        authors.orNull?.takeIf(List<String>::isNotEmpty)?.let { authors ->
            appendLine("authors:")
            authors.forEach { author -> appendLine("  - $author") }
        }
        contributors.orNull?.takeIf(List<String>::isNotEmpty)?.let { contributors ->
            appendLine("contributors:")
            contributors.forEach { contributor -> appendLine("  - $contributor") }
        }
        website.orNull?.takeIf(String::isNotBlank)?.let { website -> appendLine("website: $website") }
        foliaSupported.orNull?.takeIf { it }?.let { appendLine("folia-supported: true") }
        load.orNull?.takeIf(String::isNotBlank)?.let { appendLine("load: $it") }
        depend.orNull?.takeIf(List<String>::isNotEmpty)?.let { depend ->
            appendLine("depend:")
            depend.forEach { dependency -> appendLine("  - $dependency") }
        }
        softDepend.orNull?.takeIf(List<String>::isNotEmpty)?.let { softDepend ->
            appendLine("softdepend:")
            softDepend.forEach { softDependency -> appendLine("  - $softDependency") }
        }
        loadBefore.orNull?.takeIf(List<String>::isNotEmpty)?.let { loadBefore ->
            appendLine("loadbefore:")
            loadBefore.forEach { loadBefore -> appendLine("  - $loadBefore") }
        }
        provides.orNull?.takeIf(List<String>::isNotEmpty)?.let { provides ->
            appendLine("provides:")
            provides.forEach { provide -> appendLine("  - $provide") }
        }
    }

    /**
     * Writes the generated [text] to `plugin.yml` in [destinationDir], appending [extra]'s contents (if it exists) as extra fields
     */
    private fun writePluginYml(destinationDir: File, text: String, extra: RegularFile) {
        val output = File(destinationDir, "plugin.yml")
        output.parentFile.mkdirs()

        val existing = extra.asFile.takeIf(File::exists)?.readText()
        output.writeText(text + if (existing != null) "\n$existing" else "")
    }

    /**
     * - Lower than 1.13: 1.13
     * - 1.13-1.20.4: MAJOR.MINOR
     * - 1.20.5+: MAJOR.MINOR.PATCH
     */
    internal fun normalizeApiVersion(apiVersion: String) = when (val version = VersionNumber.parse(apiVersion)) {
        in VersionNumber.version(0, 0)..VersionNumber.parse("1.12.2") -> "1.13"
        in VersionNumber.version(1, 13)..VersionNumber.parse("1.20.4") -> "${version.major}.${version.minor}"
        else -> "${version.major}.${version.minor}.${version.patch}"
    }
}
