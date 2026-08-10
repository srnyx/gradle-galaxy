package xyz.srnyx.gradlegalaxy.extensions.dependencies

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.data.config.DependencyExtension
import xyz.srnyx.gradlegalaxy.data.config.dependency.MockBukkitExtension
import xyz.srnyx.gradlegalaxy.enums.Repository
import javax.inject.Inject


abstract class DependenciesExtension @Inject constructor(
    objects: ObjectFactory
) {
    val magicMongo: DependencyExtension = DependencyExtension(
        repositories = listOf(Repository.SRNYX_SNAPSHOTS.url, Repository.SRNYX_RELEASES.url),
        group = "xyz.srnyx",
        name = "magic-mongo",
        configurations = listOf("implementation", "testImplementation"))

    val annoyingAPI = DependencyExtension(
        repositories = listOf(Repository.SRNYX_SNAPSHOTS.url, Repository.SRNYX_RELEASES.url),
        group = "xyz.srnyx",
        name = "annoying-api",
        configurations = listOf("implementation", "testImplementation"))

    val jda = objects.newInstance(JdaExtension::class.java)

    val lazyLibrary = DependencyExtension(
        repositories = listOf(Repository.SRNYX_SNAPSHOTS.url, Repository.SRNYX_RELEASES.url),
        group = "xyz.srnyx",
        name = "lazy-library",
        configurations = listOf("implementation", "testImplementation"))

    val jUnitBom = objects.newInstance(JUnitBomExtension::class.java)

    val mockBukkit = objects.newInstance(MockBukkitExtension::class.java)

    fun magicMongo(version: String, action: DependencyExtension.() -> Unit) {
        lazyLibrary.configured = true
        magicMongo.version = version
        magicMongo.action()
    }
    fun annoyingAPI(version: String, action: DependencyExtension.() -> Unit) {
        lazyLibrary.configured = true
        annoyingAPI.version = version
        annoyingAPI.action()
    }
    fun jda(version: String, action: JdaExtension.() -> Unit) {
        lazyLibrary.configured = true
        jda.version = version
        jda.action()
    }
    fun lazyLibrary(version: String, action: DependencyExtension.() -> Unit) {
        lazyLibrary.configured = true
        lazyLibrary.version = version
        lazyLibrary.action()
    }
    fun jUnitBom(version: String, action: JUnitBomExtension.() -> Unit) {
        lazyLibrary.configured = true
        jUnitBom.version = version
        jUnitBom.action()
    }
    fun mockBukkit(version: String, action: MockBukkitExtension.() -> Unit) {
        lazyLibrary.configured = true
        mockBukkit.version = version
        mockBukkit.action()
    }

    fun process(project: Project) {
        if (magicMongo.configured) magicMongo.add(project)
        if (annoyingAPI.configured) annoyingAPI.add(project)
        if (jda.configured) jda.add(project)
        if (lazyLibrary.configured) lazyLibrary.add(project)
        if (jUnitBom.configured) jUnitBom.add(project)
        if (mockBukkit.configured) mockBukkit.add(project)
    }
}
