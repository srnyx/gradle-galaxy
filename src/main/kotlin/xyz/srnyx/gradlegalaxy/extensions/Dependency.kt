package xyz.srnyx.gradlegalaxy.extensions

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
import xyz.srnyx.gradlegalaxy.utility.hasJavaPlugin
import javax.inject.Inject


/**
 * Subclasses override [add] to layer on version-dependent behavior (e.g. [xyz.srnyx.gradlegalaxy.extensions.minecraft.PaperExtension]
 * resolving `group`/`name` from the Minecraft version) before delegating to `super.add(project)`.
 */
open class DependencyExtension @Inject constructor(
    objects: ObjectFactory,
) : Repositories() {
    @get:Input @get:Optional
    val repositories: ListProperty<String> = objects.listProperty(String::class.java)
    @get:Input
    val group: Property<String> = objects.property(String::class.java)
    @get:Input
    val name: Property<String> = objects.property(String::class.java)
    @get:Input
    val version: Property<String> = objects.property(String::class.java)
    @get:Input
    val configurations: ListProperty<String> = objects.listProperty(String::class.java).convention(listOf("compileOnly", "testImplementation"))
    /**
     * Whether this dependency is a BOM/platform (e.g. `junit-bom`) — added via [DependencyHandler.platform] instead of as a regular library
     */
    @get:Input
    val platform: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    var action: ModuleDependency.() -> Unit = {}


    fun parse(notation: String) {
        val split = notation.split(":")
        check(split.size == 3) { "Notation must be in the form 'group:name:version'" }
        group.set(split[0])
        name.set(split[1])
        version.set(split[2])
    }

    fun action(action: ModuleDependency.() -> Unit) {
        val previousAction = this.action
        this.action = {
            previousAction(this)
            action(this)
        }
    }

    internal open fun add(project: Project) {
        if (group.orNull == null || name.orNull == null || version.orNull == null) return
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }

        // Repositories
        repositories.get().forEach { project.repositories.maven(it) }

        // Add dependency
        val notation = "${group.get()}:${name.get()}:${version.get()}"
        configurations.get().forEach { configuration ->
            if (platform.get()) {
                val dependency = project.dependencies.platform(notation)
                if (dependency is ModuleDependency) action(dependency)
                project.dependencies.add(configuration, dependency)
            } else {
                project.dependencies.add(configuration, notation, action)
            }
        }
    }
}
