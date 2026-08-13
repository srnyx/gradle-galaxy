package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.extensions.discord.DiscordExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension
import xyz.srnyx.gradlegalaxy.extensions.testing.TestingExtension
import javax.inject.Inject


/**
 * Everything except [java] (see KDoc) defers actual work via [DeferredActions] so that ordering within `galaxy { }` block never matters
 */
abstract class GradleGalaxyExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory
) {
    internal val deferred = DeferredActions(project)

    // Project-wide setup
    val repository = objects.newInstance(RepositoryHolder::class.java, project)
    val java = objects.newInstance(JavaExtension::class.java)
    val minecraft = objects.newInstance(MinecraftExtension::class.java, deferred, java)
    val discord = objects.newInstance(DiscordExtension::class.java, deferred, java)
    val testing = objects.newInstance(TestingExtension::class.java, deferred)
    val publishing = objects.newInstance(PublishingExtension::class.java, deferred)

    // Pure dependencies
    val magicMongo: DependencyExtension = DependencyExtension(
        repositories = listOf("https://repo.srnyx.com/snapshots/", "https://repo.srnyx.com/releases/"),
        group = "xyz.srnyx",
        name = "magic-mongo",
        configurations = listOf("implementation", "testImplementation"))


    fun repository(action: RepositoryHolder.() -> Unit) = repository.action()
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

abstract class RepositoryHolder @Inject constructor(
    private val project: Project,
) : Repositories() {
    fun add(repositories: Iterable<String>) = repositories.forEach {
        if (it == MAVEN_LOCAL) project.repositories.mavenLocal() else project.repositories.maven(it)
    }
    fun add(vararg repositories: String) = add(repositories.asIterable())
}
