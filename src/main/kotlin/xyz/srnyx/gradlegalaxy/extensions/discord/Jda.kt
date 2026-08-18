package xyz.srnyx.gradlegalaxy.extensions.discord

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.JavaExtension
import xyz.srnyx.gradlegalaxy.utility.addCompilerArgs
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.setMainClass
import javax.inject.Inject


abstract class JdaExtension @Inject constructor(
    objects: ObjectFactory,
    private val java: JavaExtension,
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.dv8tion")
        artifact.set("JDA")
        configurations.set(listOf("implementation", "testImplementation"))
    } }
    @get:Input @get:Optional
    val excludeOpus: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input @get:Optional
    val mainClassName: Property<String> = objects.property(String::class.java)

    override fun add(project: Project) {
        if (excludeOpus.get()) action {
            exclude(module = "opus-java")
        }

        super.add(project)
    }

    internal fun setup(project: Project) {
        val extension: JdaExtension = this@JdaExtension

        check(project.hasShadowPlugin()) { "Shadow plugin is required for JDA!" }

        // Every JDA project needs these — applied unconditionally. Each has its own
        // idempotency guard, so this is a no-op wherever the consumer already triggered them
        // themselves (e.g. a separate top-level `galaxy { java { } }`).
        java.setup(project)

        project.setMainClass(extension.mainClassName.orNull)
        project.addCompilerArgs("-parameters")

        // Fix some tasks
        project.tasks["distZip"].dependsOn("shadowJar")
        project.tasks["distTar"].dependsOn("shadowJar")
        project.tasks["startScripts"].dependsOn("shadowJar")
        project.tasks["startShadowScripts"].dependsOn("jar")
    }
}
