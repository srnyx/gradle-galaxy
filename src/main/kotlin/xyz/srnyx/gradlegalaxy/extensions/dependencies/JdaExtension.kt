package xyz.srnyx.gradlegalaxy.extensions.dependencies

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.exclude
import xyz.srnyx.gradlegalaxy.data.config.DependencyExtension
import xyz.srnyx.gradlegalaxy.enums.Repository
import javax.inject.Inject


abstract class JdaExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(
    repositories = listOf(Repository.MAVEN_CENTRAL.url),
    group = "net.dv8tion",
    name = "JDA",
    configurations = listOf("implementation", "testImplementation"),
) {
    @get:Input @get:Optional
    val excludeOpus: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    override fun add(project: Project) {
        if (excludeOpus.get()) action {
            exclude(module = "opus-java")
        }

        super.add(project)
    }
}