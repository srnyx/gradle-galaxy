package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.enums.Repository
import javax.inject.Inject


abstract class DiscordExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) {
    // Pure dependencies
    val lazyLibrary: DependencyExtension = DependencyExtension(
        repositories = listOf(Repository.SRNYX_SNAPSHOTS.url, Repository.SRNYX_RELEASES.url),
        group = "xyz.srnyx",
        name = "lazy-library",
        configurations = listOf("implementation", "testImplementation"))

    // Dependency + setup merged onto one type
    val jda = objects.newInstance(JdaExtension::class.java)


    fun lazyLibrary(version: String, action: DependencyExtension.() -> Unit = {}) {
        lazyLibrary.version = version
        lazyLibrary.action()
        deferred.defer(Phase.FINALIZE) { lazyLibrary.add(project) }
    }

    fun jda(version: String, action: JdaExtension.() -> Unit = {}) {
        jda.version = version
        jda.action()
        deferred.defer(Phase.WIRE) { jda.setup(project) }
        deferred.defer(Phase.FINALIZE) { jda.add(project) }
    }
}