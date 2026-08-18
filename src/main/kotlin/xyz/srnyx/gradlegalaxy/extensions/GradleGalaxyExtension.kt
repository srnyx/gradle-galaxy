package xyz.srnyx.gradlegalaxy.extensions

import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.extensions.discord.DiscordExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension
import xyz.srnyx.gradlegalaxy.extensions.testing.TestingExtension
import xyz.srnyx.gradlegalaxy.utility.getPackage
import xyz.srnyx.gradlegalaxy.utility.makePackageSafe
import xyz.srnyx.gradlegalaxy.utility.relocate
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
    val mavenPublishing = objects.newInstance(MavenPublishingExtension::class.java)

    // Pure dependencies
    val magicMongo: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf("https://repo.srnyx.com/snapshots/", "https://repo.srnyx.com/releases/"))
        group.set("xyz.srnyx")
        artifact.set("magic-mongo")
        configurations.set(listOf("implementation", "testImplementation"))
    }


    fun getPackage() = project.getPackage()
    fun relocate(
        from: String,
        to: String = "${getPackage()}.libs.${makePackageSafe(from.split(".").last())}",
        action: SimpleRelocator.() -> Unit = {}
    ) = project.relocate(from, to, action)

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
    fun mavenPublishing(action: MavenPublishingExtension.() -> Unit) {
        mavenPublishing.action()
        deferred.defer(Phase.WIRE) { mavenPublishing.setup(project) }
    }

    fun magicMongo(version: String, action: DependencyExtension.() -> Unit = {}) {
        magicMongo.version.set(version)
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
