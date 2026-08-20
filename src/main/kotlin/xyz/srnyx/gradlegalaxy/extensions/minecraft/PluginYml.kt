package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.util.internal.VersionNumber
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.utility.getPackage
import xyz.srnyx.gradlegalaxy.utility.makePackageSafe
import java.io.File
import javax.inject.Inject


abstract class PluginYmlExtension @Inject constructor(
    private val project: Project,
    private val objects: ObjectFactory,
    minecraft: MinecraftExtension,
) {
    // DeveloperData
    @Used val SRNYX: DeveloperData = DeveloperData.srnyx
    @Used val DKIM19375: DeveloperData = DeveloperData.dkim19375
    // load
    @Used val STARTUP = "STARTUP"
    @Used val POSTWORLD = "POSTWORLD"

    /**
     * Defaults to project name
     */
    @get:Input @get:Optional
    val name: Property<String> = objects.property(String::class.java).convention(project.provider { project.name })
    /**
     * Defaults to project version
     */
    @get:Input @get:Optional
    val version: Property<String> = objects.property(String::class.java).convention(project.provider { project.version.toString() })
    /**
     * Defaults to project description
     */
    @get:Input @get:Optional
    val description: Property<String> = objects.property(String::class.java).convention(project.provider { project.description })
    /**
     * Defaults to `mainPackage.name`
     */
    @get:Input @get:Optional
    val main: Property<String> = objects.property(String::class.java).convention(project.provider { "${project.getPackage()}.${project.name}" })
    /**
     * Defaults to Minecraft version from Paper/Spigot
     */
    @get:Input @get:Optional
    val apiVersion: Property<String> = objects.property(String::class.java).convention(project.provider { minecraft.getMinecraftVersion() })
    @get:Input @get:Optional
    val authors: ListProperty<String> = objects.listProperty(String::class.java)
    /**
     * Paper-only
     */
    @get:Input @get:Optional
    val contributors: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val website: Property<String> = objects.property(String::class.java)
    /**
     * Folia-only
     */
    @get:Input @get:Optional
    val foliaSupported: Property<Boolean> = objects.property(Boolean::class.java).convention(minecraft.folia)
    @get:Input @get:Optional
    val load: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val depend: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val softDepend: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val loadBefore: ListProperty<String> = objects.listProperty(String::class.java)
    /**
     * Paper-only
     */
    @get:Input @get:Optional
    val provides: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input @get:Optional
    val libraries: ListProperty<String> = objects.listProperty(String::class.java)
    /**
     * Paper-only
     */
    @get:Input @get:Optional
    val defaultPermission: Property<String> = objects.property(String::class.java)
    /**
     * The prefix for permissions that have prefix enabled
     */
    @get:Input
    val permissionPrefix: Property<String> = objects.property(String::class.java).convention(makePackageSafe(project.name) + ".")
    @get:Input
    val commands: ListProperty<Command> = objects.listProperty(Command::class.java).convention(emptyList())
    @get:Input
    val permissions: ListProperty<Permission> = objects.listProperty(Permission::class.java).convention(emptyList())


    /**
     * Adds [DeveloperData.id] to [authors] and sets [website]
     */
    @Used
    fun developerData(data: DeveloperData) {
        data.id?.let { authors.add(it) }
        data.url?.let { website.set(it) }
    }

    @Used
    fun command(name: String, action: Command.() -> Unit = {}) {
        val command: Command = objects.newInstance(Command::class.java, this, name)
        command.action()
        commands.add(command)
    }

    @Used
    fun permission(name: String, action: Permission.() -> Unit = {}) {
        val permission: Permission = objects.newInstance(Permission::class.java)
        permission.permission.set(name)
        permission.action()
        permissions.add(permission)
    }

    internal fun setup(project: Project) {
        setupTask(project, "main", "processResources")
        setupTask(project, "test", "processTestResources", "Mock")
    }

    private fun setupTask(
        project: Project,
        module: String,
        processTask: String,
        prefixName: String = "",
    ) {
        val moduleCapital = module.replaceFirstChar { it.uppercase() }

        // Add prefix to name and main class name
        val nameValue: String = prefixName + name.get()
        var mainValue: String = main.get()
        if (prefixName.isNotBlank()) mainValue = "${mainValue.substringBeforeLast(".")}.$prefixName${mainValue.substringAfterLast(".")}"

        // Setup
        val extra = project.layout.projectDirectory.file("src/$module/resources/plugin.yml")
        val textValue: String by lazy { buildText(nameValue, mainValue) }
        val text = project.provider { textValue }
        val generated = project.layout.buildDirectory.dir("generated/pluginYml/$module")
        val generatePluginYml = project.tasks.register("generate${moduleCapital}PluginYml") {
            val output = generated.map { it.file("plugin.yml") }
            outputs.file(output)
            inputs.property("pluginYmlText", text)
            inputs.files(extra).optional(true)

            doLast {
                val output = File(output.get().asFile.parentFile, "plugin.yml")
                output.parentFile.mkdirs()

                val existing = extra.asFile.takeIf(File::exists)?.readText()
                output.writeText(text.get() + if (existing != null) "\n$existing" else "")
            }
        }
        project.tasks.named<ProcessResources>(processTask) {
            dependsOn(generatePluginYml)
            exclude { it.file == extra.asFile }
            from(generated)
        }
    }

    /**
     * Builds the `plugin.yml` text, using [nameValue]/[mainValue] instead of [name]/[main] so the `Mock`-prefixed test variant can reuse it
     */
    private fun buildText(nameValue: String, mainValue: String): String = buildString {
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
        libraries.orNull?.takeIf(List<String>::isNotEmpty)?.let { libraries ->
            appendLine("libraries:")
            libraries.forEach { library -> appendLine("  - $library") }
        }
        defaultPermission.orNull?.let { appendLine("default-permission: $it") }
        commands.get().takeIf(List<Command>::isNotEmpty)?.let { commands ->
            appendLine("commands:")
            commands.forEach { command ->
                appendLine("  ${command.name}:")
                command.aliases.orNull?.takeIf(List<String>::isNotEmpty)?.let { aliases ->
                    appendLine("    aliases:")
                    aliases.forEach { alias -> appendLine("      - $alias") }
                }
                command.description.orNull?.let { appendLine("    description: $it") }
                command.usage.orNull?.let { appendLine("    usage: $it") }
                command.permission.orNull?.let { appendLine("    permission: ${prefixPermission(it.permission.get())}") }
                command.permissionMessage.orNull?.let { appendLine("    permission-message: $it") }
            }
        }
        permissions.get().takeIf(List<Permission>::isNotEmpty)?.let { permissions ->
            appendLine("permissions:")
            val seen = mutableSetOf<String>()
            permissions.forEach { permission ->
                // Prevent duplicates
                val name = prefixPermission(permission.permission.get())
                if (!seen.add(name)) {
                    project.logger.warn("[$nameValue] Duplicate permission '$name' found in plugin.yml, only keeping first")
                    return@forEach
                }

                appendLine("  $name:")
                permission.description.orNull?.let { appendLine("    description: $it") }
                permission.default.orNull?.let { appendLine("    default: $it") }
                permission.children.orNull?.takeIf(Map<String, Boolean>::isNotEmpty)?.let { children ->
                    appendLine("    children:")
                    children.forEach { (child, value) -> appendLine("      $child: $value") }
                }
            }
        }
    }

    /**
     * - Lower than 1.13: 1.13
     * - 1.13-1.20.4: MAJOR.MINOR
     * - 1.20.5+: MAJOR.MINOR.PATCH
     */
    private fun normalizeApiVersion(apiVersion: String) = when (val version = VersionNumber.parse(apiVersion)) {
        in VersionNumber.version(0, 0)..VersionNumber.parse("1.12.2") -> "1.13"
        in VersionNumber.version(1, 13)..VersionNumber.parse("1.20.4") -> "${version.major}.${version.minor}"
        else -> "${version.major}.${version.minor}.${version.patch}"
    }

    internal fun prefixPermission(permission: String) = (if (permissionPrefix.orNull != null) "${permissionPrefix.get()}." else "") + permission
}

abstract class Command @Inject constructor(
    private val pluginYml: PluginYmlExtension,
    private val objects: ObjectFactory,
    val name: String,
) {
    @get:Input @get:Optional
    val aliases: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input
    val description: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val usage: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val permission: Property<Permission> = objects.property(Permission::class.java)
    @get:Input @get:Optional
    val permissionMessage: Property<String> = objects.property(String::class.java)


    @Used
    fun permission(permission: String, action: Permission.() -> Unit = {}) {
        val permissionBuilder: Permission = objects.newInstance(Permission::class.java)
        permissionBuilder.permission.set(permission)
        permissionBuilder.description.set("Allows the player to use /$name")
        permissionBuilder.action()
        this.permission.set(permissionBuilder)
        pluginYml.permissions.add(permissionBuilder)
    }
}

abstract class Permission @Inject constructor(
    objects: ObjectFactory,
) {
    @Used val TRUE = "true"
    @Used val FALSE = "false"
    @Used val OP = "op"
    @Used val NOT_OP = "not op"

    @get:Input
    val prefix: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val permission: Property<String> = objects.property(String::class.java)
    @get:Input
    val description: Property<String> = objects.property(String::class.java)
    @get:Input
    val default: Property<String> = objects.property(String::class.java)
    @get:Input
    val children: MapProperty<String, Boolean> = objects.mapProperty(String::class.java, Boolean::class.java)
}
