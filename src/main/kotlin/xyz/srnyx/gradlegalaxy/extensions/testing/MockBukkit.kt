package xyz.srnyx.gradlegalaxy.extensions.testing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import javax.inject.Inject


abstract class MockBukkitExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.PAPER))
        group.set("com.github.seeseemelk")
        name.set("MockBukkit-v")
        configurations.set(listOf("testImplementation"))
    } }
    @get:Input
    val minecraftVersion: Property<String> = objects.property(String::class.java).convention("1.20")

    override fun add(project: Project) {
        name.set("$name${minecraftVersion.get()}")

        // Exclude spigot-api from test classpath so MockBukkit's Paper takes precedence
        project.configurations.named("testImplementation") { exclude("org.spigotmc", "spigot-api") }

        super.add(project)
    }
}
