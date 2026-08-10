package xyz.srnyx.gradlegalaxy.extensions.setup

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.named
import xyz.jpenilla.runpaper.task.RunServer
import xyz.srnyx.gradlegalaxy.utility.hasRunPaperPlugin
import javax.inject.Inject


abstract class RunPaperExtension @Inject constructor(
    objects: ObjectFactory
) {
    var configured: Boolean = false

    @get:Input @get:Optional
    val minecraftVersion: Property<String> = objects.property(String::class.java).convention("1.21.11")
    @get:Input @get:Optional
    var autoAcceptEula: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    fun setup(project: Project) {
        // Setup default Run-Paper task with 1.21.11.
        // This can be changed per-consumer with tasks { runServer { minecraftVersion("VERSION") } } (and other options)
        if (project.hasRunPaperPlugin()) {
            // Set eula=true in run/eula.txt
            val acceptEula = if (!autoAcceptEula.get()) null else project.tasks.register("acceptEula") {
                group = "run paper"
                description = "Automatically accepts the EULA"

                val eulaTxt = project.layout.projectDirectory.file("run/eula.txt")
                outputs.file(eulaTxt)
                doLast {
                    eulaTxt.asFile.parentFile.mkdirs()
                    eulaTxt.asFile.writeText("eula=true")
                }
            }

            project.tasks.named<RunServer>("runServer") {
                if (acceptEula != null) dependsOn(acceptEula)

                // Only set default if not already set by consumer
                if (!version.isPresent) minecraftVersion(minecraftVersion.get())
            }
        }
    }
}