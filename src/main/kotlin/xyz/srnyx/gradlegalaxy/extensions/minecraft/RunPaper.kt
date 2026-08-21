package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.named
import xyz.jpenilla.runpaper.task.RunServer
import xyz.srnyx.gradlegalaxy.utility.hasRunPaperPlugin


class RunPaperExtension(objects: ObjectFactory) {
    @get:Input
    val minecraftVersion: Property<String> = objects.property(String::class.java).convention("1.21.11")
    @get:Input
    val autoAcceptEula: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val serverProperties: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(mapOf(
        "allow-flight" to "true",
        "enable-command-block" to "true"))
    /**
     * The JDK used to *launch* the `runServer` process — independent of the project's compile [javaVersion][xyz.srnyx.gradlegalaxy.extensions.JavaExtension.javaVersion].
     * Defaults to 21 since newer JDKs can run older bytecode fine, so this covers old and new Paper versions alike.
     */
    @get:Input
    val javaVersion: Property<JavaVersion> = objects.property(JavaVersion::class.java).convention(JavaVersion.VERSION_21)

    var action: (RunServer.() -> Unit)? = null

    private var applied = false


    fun action(action: RunServer.() -> Unit) {
        this.action = action
    }

    /**
     * Idempotent: multiple `galaxy { }` entries (top-level `runPaper { }`, and any entry that bundles
     * runPaper setup in automatically, like `annoyingAPI { }`) may all call this. Only the first actually applies.
     */
    internal fun setup(project: Project) {
        if (applied) return
        applied = true

        // Setup default Run-Paper task with 1.21.11.
        // This can be changed per-consumer with tasks { runServer { minecraftVersion("VERSION") } } (and other options)
        if (project.hasRunPaperPlugin()) {
            // Set eula=true in run/eula.txt
            val acceptEula = project.tasks.register("acceptEula") {
                group = "run paper"
                description = "Automatically accepts the EULA"

                val eulaTxt = project.layout.projectDirectory.file("run/eula.txt")
                outputs.file(eulaTxt)

                doLast {
                    eulaTxt.asFile.parentFile.mkdirs()
                    eulaTxt.asFile.writeText("eula=true")
                }
            }

            // Apply server properties
            val applyServerProperties = project.tasks.register("applyServerProperties") {
                group = "run paper"
                description = "Applies default properties to the server.properties file"

                val serverPropertiesFile = project.layout.projectDirectory.file("run/server.properties")
                inputs.property("serverProperties", serverProperties)
                outputs.file(serverPropertiesFile)

                doLast {
                    val file = serverPropertiesFile.asFile
                    file.parentFile.mkdirs()

                    val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
                    val remaining = serverProperties.get().toMutableMap()

                    for (i in lines.indices) {
                        val key = lines[i].substringBefore('=').trim()
                        val value = remaining.remove(key) ?: continue
                        lines[i] = "$key=$value"
                    }
                    remaining.forEach { (key, value) -> lines += "$key=$value" }

                    file.writeText(lines.joinToString("\n"))
                }
            }

            val toolchains = project.extensions.getByType(JavaToolchainService::class.java)
            project.tasks.named<RunServer>("runServer") {
                if (autoAcceptEula.get()) dependsOn(acceptEula)
                if (serverProperties.get().isNotEmpty()) dependsOn(applyServerProperties)

                // Only set default if not already set by consumer
                if (!version.isPresent) minecraftVersion(minecraftVersion.get())

                // Launch with a JDK new enough for the downloaded Paper jar, regardless of whichever JDK is running Gradle
                javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(this@RunPaperExtension.javaVersion.get().majorVersion)) })

                // Custom action
                action?.invoke(this)
            }
        }
    }
}
