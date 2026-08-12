package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.utility.addCompilerArgs
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.setMainClass
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

        project.setMainClass(extension.mainClassName.orNull)
        project.addCompilerArgs("-parameters")

        // Fix some tasks
        project.tasks["distZip"].dependsOn("shadowJar")
        project.tasks["distTar"].dependsOn("shadowJar")
        project.tasks["startScripts"].dependsOn("shadowJar")
        project.tasks["startShadowScripts"].dependsOn("jar")
    }
}
