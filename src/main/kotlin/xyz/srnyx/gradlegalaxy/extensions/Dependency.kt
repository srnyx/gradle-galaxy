package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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
    var repositories: List<String>,
    var group: String,
    var name: String,
    var configurations: List<String>,
    /** Whether this dependency is a BOM/platform (e.g. `junit-bom`) — added via [DependencyHandler.platform] instead of as a regular library. */
    var platform: Boolean = false,
) {
    @get:Input
    val version: Property<String> = objects.property(String::class.java)
    var action: ModuleDependency.() -> Unit = {}

    fun action(action: ModuleDependency.() -> Unit) {
        val previousAction = this.action
        this.action = {
            previousAction(this)
            action(this)
        }
    }

    internal open fun add(project: Project) {
        check(version.isPresent) { "Dependency version is not configured!" }
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }

        // Repositories
        repositories.forEach { project.repositories.maven(it) }

        // Add dependency
        val notation = "${group}:${name}:${version.get()}"
        configurations.forEach { configuration ->
            if (platform) {
                val dependency = project.dependencies.platform(notation)
                if (dependency is ModuleDependency) action(dependency)
                project.dependencies.add(configuration, dependency)
            } else {
                project.dependencies.add(configuration, notation, action)
            }
        }
    }
}
