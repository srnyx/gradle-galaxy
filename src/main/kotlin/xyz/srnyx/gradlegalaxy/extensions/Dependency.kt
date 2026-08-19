package xyz.srnyx.gradlegalaxy.extensions

import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.annoyingapi.Relocation
import xyz.srnyx.gradlegalaxy.utility.getPackage
import xyz.srnyx.gradlegalaxy.utility.hasJavaPlugin
import xyz.srnyx.gradlegalaxy.utility.relocate
import javax.inject.Inject
import kotlin.text.split


/**
 * Subclasses override [add] to layer on version-dependent behavior (e.g. [xyz.srnyx.gradlegalaxy.extensions.minecraft.PaperExtension]
 * resolving `group`/`artifact` from the Minecraft version) before delegating to `super.add(project)`.
 */
open class DependencyExtension @Inject constructor(
    private val objects: ObjectFactory,
) : Repositories() {
    @get:Input @get:Optional
    val repositories: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input
    val group: Property<String> = objects.property(String::class.java)
    @get:Input
    val artifact: Property<String> = objects.property(String::class.java)
    @get:Input
    val version: Property<String> = objects.property(String::class.java)
    @get:Input
    val configurations: ListProperty<String> = objects.listProperty(String::class.java).convention(listOf("compileOnly", "testImplementation"))
    /**
     * Whether this dependency is a BOM/platform (e.g. `junit-bom`) — added via [DependencyHandler.platform] instead of as a regular library
     */
    @get:Input
    val platform: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    @get:Input
    val applyRelocations: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val relocations: ListProperty<RelocateExtension> = objects.listProperty(RelocateExtension::class.java).convention(emptyList())

    var action: ModuleDependency.() -> Unit = {}


    @Used
    fun parse(notation: String) {
        val split = notation.split(":")
        check(split.size == 3) { "Notation must be in the form 'group:artifact:version'" }
        group.set(split[0])
        artifact.set(split[1])
        version.set(split[2])
    }

    fun relocate(action: RelocateExtension.() -> Unit = {}) {
        val relocation = objects.newInstance(RelocateExtension::class.java, this)
        relocation.action()
        relocations.add(relocation)
    }
    @Used
    fun relocate(from: String, to: String? = null) = relocate {
        this.from.set(from)
        to?.let { this.to.set(it) }
    }

    fun action(action: ModuleDependency.() -> Unit) {
        val previousAction = this.action
        this.action = {
            previousAction(this)
            action(this)
        }
    }

    internal fun conventionCopy(other: DependencyExtension) {
        repositories.convention(other.repositories)
        group.convention(other.group)
        artifact.convention(other.artifact)
        version.convention(other.version)
        configurations.convention(other.configurations)
        platform.convention(other.platform)
        relocations.convention(other.relocations)
    }

    internal open fun add(project: Project) {
        if (group.orNull == null || artifact.orNull == null || version.orNull == null) return
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }
        val versionValue = version.get()

        // Repositories (dev/snapshot = add mavenLocal())
        if (versionValue == "dev" || versionValue == "snapshot") project.repositories.mavenLocal()
        repositories.get().forEach { project.repositories.maven(it) }

        // Add dependency
        val notation = "${group.get()}:${artifact.get()}:${versionValue}"
        configurations.get().forEach { configuration ->
            if (platform.get()) {
                val dependency = project.dependencies.platform(notation)
                if (dependency is ModuleDependency) action(dependency)
                project.dependencies.add(configuration, dependency)
            } else {
                project.dependencies.add(configuration, notation, action)
            }
        }

        // Relocations
        if (applyRelocations.get()) relocations.get().forEach { relocation -> relocation.apply() }
    }
}

abstract class RelocateExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory,
    dependency: DependencyExtension,
) {
    @get:Input
    val from: Property<String> = objects.property(String::class.java).convention(dependency.group)
    /**
     * Null/empty = `{package}.libs.<lastSegmentOf(from)>`
     */
    @get:Input @get:Optional
    val to: Property<String> = objects.property(String::class.java)

    var action: SimpleRelocator.() -> Unit = {}


    fun getTo(): String =
        (to.orNull ?: "{package}.libs.${from.get().split(".").last()}").replace("{package}", project.getPackage())

    fun action(action: SimpleRelocator.() -> Unit) {
        val previousAction = this.action
        this.action = {
            previousAction(this)
            action(this)
        }
    }

    fun toData(): Relocation = Relocation(
        from = from.get(),
        to = to.orNull)

    internal fun apply() {
        project.relocate(from.get(), getTo(), action)
    }
}
