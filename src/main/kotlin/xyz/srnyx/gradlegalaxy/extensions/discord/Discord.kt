package xyz.srnyx.gradlegalaxy.extensions.discord

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.extensions.DeferredActions
import xyz.srnyx.gradlegalaxy.extensions.JavaExtension
import xyz.srnyx.gradlegalaxy.extensions.Phase
import javax.inject.Inject


abstract class DiscordExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    private val java: JavaExtension,
    objects: ObjectFactory,
) {
    // Dependency + setup merged onto one type
    val jda = objects.newInstance(JdaExtension::class.java, java)
    val lazyLibrary = objects.newInstance(LazyLibraryExtension::class.java, project, java)


    fun jda(version: String, action: JdaExtension.() -> Unit = {}) {
        jda.version.set(version)
        jda.action()
        deferred.defer(Phase.WIRE) { jda.setup(project) }
        deferred.defer(Phase.FINALIZE) { jda.add(project) }
    }
    fun lazyLibrary(version: String, action: LazyLibraryExtension.() -> Unit = {}) {
        lazyLibrary.version.set(version)
        lazyLibrary.action()
        deferred.defer(Phase.WIRE) { lazyLibrary.setup() }
        deferred.defer(Phase.FINALIZE) { lazyLibrary.add(project) }
    }
}