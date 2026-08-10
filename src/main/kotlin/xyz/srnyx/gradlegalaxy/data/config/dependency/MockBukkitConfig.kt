package xyz.srnyx.gradlegalaxy.data.config.dependency

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.data.config.DependencyExtension
import xyz.srnyx.gradlegalaxy.enums.Repository
import javax.inject.Inject


open class MockBukkitConfig(
    var group: String = "com.github.seeseemelk",
    var minecraftVersion: String = "1.20",
) {
    fun toExtension(): MockBukkitExtension.() -> Unit = {
        val config: MockBukkitConfig = this@MockBukkitConfig

        group = config.group
        minecraftVersion.set(config.minecraftVersion)
    }
}


abstract class MockBukkitExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(
    repositories = listOf(Repository.MAVEN_CENTRAL.url, Repository.PAPER.url),
    group = "com.github.seeseemelk",
    name = "MockBukkit-v",
    configurations = listOf("testImplementation"),
) {
    @get:Input
    val minecraftVersion: Property<String> = objects.property(String::class.java).convention("1.20")

    override fun add(project: Project) {
        name = "$name${minecraftVersion.get()}"

        // Exclude spigot-api from test classpath so MockBukkit's Paper takes precedence
        project.configurations.named("testImplementation") { exclude("org.spigotmc", "spigot-api") }

        super.add(project)
    }
}
