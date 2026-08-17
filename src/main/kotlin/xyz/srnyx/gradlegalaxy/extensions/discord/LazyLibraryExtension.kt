package xyz.srnyx.gradlegalaxy.extensions.discord

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import xyz.srnyx.gradlegalaxy.extensions.JavaExtension
import xyz.srnyx.gradlegalaxy.utility.hasJavaPlugin
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import javax.inject.Inject


abstract class LazyLibraryExtension @Inject internal constructor(
    private val project: Project,
    private val java: JavaExtension,
    objects: ObjectFactory,
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.SRNYX_SNAPSHOTS, REPOSITORIES.SRNYX_RELEASES))
        group.set("xyz.srnyx")
        name.set("lazy-library")
        configurations.set(listOf("implementation", "testImplementation"))
    } }
    fun setup() {
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }
        check(project.hasShadowPlugin()) { "Shadow plugin is required for Lazy Library!" }

        // Every Lazy Library project needs these — applied unconditionally. Each has its own
        // idempotency guard, so this is a no-op wherever the consumer already triggered them
        // themselves (e.g. a separate top-level `galaxy { java { } }`).
        java.setup(project)
    }
}