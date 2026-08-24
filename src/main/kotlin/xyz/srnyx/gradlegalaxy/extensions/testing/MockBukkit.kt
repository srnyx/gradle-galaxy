package xyz.srnyx.gradlegalaxy.extensions.testing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import javax.inject.Inject


abstract class MockBukkitExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.PAPER))
        group.set("com.github.seeseemelk")
        artifact.set("MockBukkit-v")
        configurations.set(listOf("testImplementation"))
    } }
    @get:Input
    val minecraftVersion: Property<String> = objects.property(String::class.java).convention("1.20")

    override fun add(project: Project) {
        artifact.set("${artifact.get()}${minecraftVersion.get()}")

        // Exclude the project's own server API from the test classpath so MockBukkit's own
        // (transitive, version-matched) Spigot/Paper APIs takes precedence instead.
        // `io.papermc.paper:paper-api` itself is deliberately NOT excluded: projects that
        // already use it (Paper 1.17+) shares MockBukkit's own coordinate, so normal Gradle
        // version-conflict resolution already picks MockBukkit's newer version.
        project.configurations.named("testImplementation") {
            exclude("org.spigotmc", "spigot-api")
            exclude("org.spigotmc", "spigot")
            exclude("com.destroystokyo.paper", "paper-api")
            exclude("org.github.paperspigot", "paperspigot-api")
        }

        super.add(project)
    }
}
