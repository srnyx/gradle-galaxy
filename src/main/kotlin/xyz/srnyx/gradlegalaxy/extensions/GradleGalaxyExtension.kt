package xyz.srnyx.gradlegalaxy.extensions

import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.annoyingapi.Exclude
import xyz.srnyx.gradlegalaxy.extensions.discord.DiscordExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.RuntimeLibraryExtension
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
    val dependency = objects.newInstance(DependenciesExtension::class.java, project, objects)
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

    @Used
    fun relocate(
        from: String,
        to: String = "${getPackage()}.libs.${makePackageSafe(from.split(".").last())}",
        action: SimpleRelocator.() -> Unit = {}
    ) = project.relocate(from, to, action)

    fun json(action: JsonBuilder.() -> Unit) = Json { action() }

    @Used
    fun annoyingMetadata(action: AnnoyingMetadataBuilder.() -> Unit): AnnoyingMetadata {
        val builder = project.objects.newInstance(AnnoyingMetadataBuilder::class.java)
        builder.action()
        return builder.build()
    }

    fun repository(action: RepositoryHolder.() -> Unit) = repository.action()
    fun dependency(action: DependenciesExtension.() -> Unit) = dependency.action()
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

abstract class DependenciesExtension @Inject constructor(
    private val project: Project,
    private val objects: ObjectFactory,
) : Repositories() {
    fun add(action: DependencyExtension.() -> Unit) {
        val dependency = objects.newInstance(DependencyExtension::class.java)
        dependency.action()
        dependency.add(project)
    }

    fun add(notation: String, action: DependencyExtension.() -> Unit) = add {
        parse(notation)
        action()
    }
}

abstract class AnnoyingMetadataBuilder @Inject constructor(
    objects: ObjectFactory
) : Repositories() {
    @get:Input
    val packageName: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val javaVersion: Property<Int> = objects.property(Int::class.java)
    @get:Input
    val repositories: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    @get:Input
    val runtimeLibraries: ListProperty<RuntimeLibraryExtension> = objects.listProperty(RuntimeLibraryExtension::class.java).convention(emptyList())
    @get:Input
    val excludes: ListProperty<Exclude> = objects.listProperty(Exclude::class.java).convention(emptyList())


    fun exclude(group: String, artifact: String) = excludes.add(Exclude(group, artifact))

    fun build() = AnnoyingMetadata(
        packageName = packageName.get(),
        javaVersion = javaVersion.orNull,
        repositories = repositories.get().distinct(),
        runtimeLibraries = runtimeLibraries.get().map { it.toData() },
        excludes = excludes.get(),
    )
}
