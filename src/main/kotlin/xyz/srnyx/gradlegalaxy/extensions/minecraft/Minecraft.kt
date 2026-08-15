package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.extensions.DeferredActions
import xyz.srnyx.gradlegalaxy.extensions.JavaExtension
import xyz.srnyx.gradlegalaxy.extensions.Phase
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import javax.inject.Inject


abstract class MinecraftExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    private val java: JavaExtension,
    objects: ObjectFactory,
) {
    //TODO generate majority of plugin.yml automatically instead of using replacements
    @get:Input @get:Optional
    var replacementFiles: SetProperty<String> = objects.setProperty(String::class.java).convention(listOf("plugin.yml"))
    @get:Input @get:Optional
    var replacements: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(mapOf("defaultReplacements" to "true"))

    // Project-wide setup
    val pluginYml = objects.newInstance(PluginYmlExtension::class.java)
    val runPaper = RunPaperExtension(objects)
    val adventure = objects.newInstance(AdventureExtension::class.java, deferred)
    // Pure dependencies
    val spigotAPI = objects.newInstance(SpigotApiExtension::class.java)
    val spigotNMS = objects.newInstance(SpigotNmsExtension::class.java)
    val paper = objects.newInstance(PaperExtension::class.java)
    // Dependency + setup merged onto one type
    val annoyingAPI = objects.newInstance(AnnoyingApiExtension::class.java, java, this)

    private var applied = false


    @Used
    fun pluginYml(action: PluginYmlExtension.() -> Unit) {
        pluginYml.action()
        deferred.defer(Phase.WIRE) { pluginYml.setup(project, this) }
    }
    @Used
    fun runPaper(action: RunPaperExtension.() -> Unit = {}) {
        runPaper.action()
        deferred.defer(Phase.WIRE) { runPaper.setup(project) }
    }
    @Used
    fun adventure(action: AdventureExtension.() -> Unit) = adventure.action()

    fun spigotAPI(version: String, action: SpigotApiExtension.() -> Unit = {}) {
        spigotAPI.version.set(version)
        spigotAPI.action()
        deferred.defer(Phase.WIRE) { spigotAPI.setup(project) }
        deferred.defer(Phase.FINALIZE) { spigotAPI.add(project) }
    }
    fun spigotNMS(version: String, action: SpigotNmsExtension.() -> Unit = {}) {
        spigotNMS.version.set(version)
        spigotNMS.action()
        deferred.defer(Phase.WIRE) { spigotNMS.setup(project) }
        deferred.defer(Phase.FINALIZE) { spigotNMS.add(project) }
    }
    fun paper(version: String, action: PaperExtension.() -> Unit = {}) {
        paper.version.set(version)
        paper.action()
        deferred.defer(Phase.WIRE) { paper.setup(project) }
        deferred.defer(Phase.FINALIZE) { paper.add(project) }
    }

    fun annoyingAPI(version: String, action: AnnoyingApiExtension.() -> Unit = {}) {
        annoyingAPI.version.set(version)
        annoyingAPI.action()
        deferred.defer(Phase.WIRE) { annoyingAPI.setup(project) }
        deferred.defer(Phase.FINALIZE) { annoyingAPI.add(project) }
    }

    fun getMinecraftVersion(): String? = when {
        paper.version.orNull != null -> paper.version.get()
        spigotAPI.version.orNull != null -> spigotAPI.version.get()
        spigotNMS.version.orNull != null -> spigotNMS.version.get()
        else -> null
    }

    /**
     * Idempotent: multiple `galaxy { }` entries (top-level `minecraft { }`, and any entry that bundles
     * minecraft setup in automatically, like `annoyingAPI { }`) may all call this. Only the first actually applies.
     */
    internal fun setup(project: Project) {
        if (applied) return
        applied = true

        // Every Minecraft project needs these — applied unconditionally. Each has its own
        // idempotency guard, so this is a no-op wherever the consumer already triggered them
        // themselves (e.g. a separate top-level `galaxy { java { } }`).
        java.setup(project)

        if (replacementFiles.orNull != null && replacements.orNull != null) {
            project.addReplacementsTask(replacementFiles.get(), replacements.get())
        }
    }
}
