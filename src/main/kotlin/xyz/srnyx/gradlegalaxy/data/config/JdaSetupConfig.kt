package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.get
import xyz.srnyx.gradlegalaxy.utility.addCompilerArgs
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.setMainClass
import javax.inject.Inject


/**
 * Configuration for [xyz.srnyx.gradlegalaxy.utility.setupJda]
 *
 * @param mainClassName The main class name of the project (example: `xyz.srnyx.lazylibrary.LazyLibrary`)
 * @param excludeOpus Whether to exclude the `opus-java` dependency from JDA (
 */
open class JdaSetupConfig(
    var mainClassName: String? = null,
    var excludeOpus: Boolean = true,
) {
    internal fun toExtension(): JdaSetupExtension.() -> Unit = {
        val config: JdaSetupConfig = this@JdaSetupConfig

        mainClassName.set(config.mainClassName)
//        excludeOpus.set(config.excludeOpus) //TODO
    }
}

abstract class JdaSetupExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var configured: Boolean = false

    @get:Input @get:Optional
    val mainClassName: Property<String> = objects.property(String::class.java)

    fun setup(project: Project) {
        val extension: JdaSetupExtension = this@JdaSetupExtension

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
