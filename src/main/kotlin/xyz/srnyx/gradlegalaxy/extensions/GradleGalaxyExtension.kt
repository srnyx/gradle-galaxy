package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import javax.inject.Inject


/**
 * Everything except [java] (see KDoc) defers actual work via [DeferredActions] so that ordering within `galaxy { }` block never matters
 */
abstract class GradleGalaxyExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory
) {
    private val deferred = DeferredActions(project)

    // Project-wide setup
    val java = objects.newInstance(JavaExtension::class.java)
    val minecraft = objects.newInstance(MinecraftExtension::class.java, deferred, java)
    val discord = objects.newInstance(DiscordExtension::class.java, deferred)
    val testing = objects.newInstance(TestingExtension::class.java, deferred)
    val publishing = objects.newInstance(PublishingExtension::class.java, deferred)

    // Pure dependencies
    val magicMongo: DependencyExtension = DependencyExtension(
        repositories = listOf(Repository.SRNYX_SNAPSHOTS.url, Repository.SRNYX_RELEASES.url),
        group = "xyz.srnyx",
        name = "magic-mongo",
        configurations = listOf("implementation", "testImplementation"))


    fun repository(vararg repository: Any) = repository.forEach {
        // Repository has special handling, can't just use toString on it
        if (it is Repository) {
            project.repository(it)
        } else {
            project.repository(it.toString())
        }
    }

    fun java(action: JavaExtension.() -> Unit) {
        java.action()
        java.setup(project) // eager: only writes plain Project state, see JavaExtension's KDoc
    }
    fun minecraft(action: MinecraftExtension.() -> Unit) {
        minecraft.action()
        deferred.defer(Phase.WIRE) { minecraft.setup(project) }
    }
    fun discord(action: DiscordExtension.() -> Unit) = discord.action()
    fun testing(action: TestingExtension.() -> Unit) = testing.action()
    fun publishing(action: PublishingExtension.() -> Unit) = publishing.action()

    fun magicMongo(version: String, action: DependencyExtension.() -> Unit = {}) {
        magicMongo.version = version
        magicMongo.action()
        deferred.defer(Phase.FINALIZE) { magicMongo.add(project) }
    }
}
