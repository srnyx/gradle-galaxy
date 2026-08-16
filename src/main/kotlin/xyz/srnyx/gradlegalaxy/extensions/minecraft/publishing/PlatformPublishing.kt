package xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.ReleaseChannel
import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension
import xyz.srnyx.gradlegalaxy.utility.addPlatformsResourceFileTask
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.hasHangarPublishPlugin
import xyz.srnyx.gradlegalaxy.utility.hasModPublishPlugin
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.inGitHubPreRelease
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow


class PlatformPublishingExtension(
    private val project: Project,
    internal val minecraft: MinecraftExtension,
    objects: ObjectFactory,
) {
    /**
     * Needs explicit addition
     */
    @Used val FOLIA = "folia"
    @Used val PURPUR = "purpur"
    @Used val PAPER = "paper"
    @Used val SPIGOT = "spigot"
    @Used val BUKKIT = "bukkit"
    @Used val FABRIC = "fabric"
    /**
     * Needs explicit addition
     */
    @Used val QUILT = "quilt"
    @Used val FORGE = "forge"
    @Used val NEOFORGE = "neoforge"

    /**
     * Local, gradle-galaxy-side tier -> concrete-loaders expansion,
     * used only to compute *this release's* `mod-publish-plugin` loaders list.
     */
    private val tierExpansions: Map<String, Set<String>> = mapOf(
        BUKKIT to setOf(SPIGOT, PAPER, PURPUR),
        SPIGOT to setOf(PAPER, PURPUR),
        PAPER to setOf(PURPUR))

    @get:Input
    val platforms: MapProperty<PluginPlatform, String> = objects.mapProperty(PluginPlatform::class.java, String::class.java)
    /**
     * Defaults to Paper/Spigot Minecraft version
     */
    @get:Input @get:Optional
    val minecraftVersionStart: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val minecraftVersionEnd: Property<String> = objects.property(String::class.java)
    /**
     * The API tier(s) this plugin is compiled against (e.g. [SPIGOT]) — auto-expands to every loader built on
     * top of that tier (see [tierExpansions]/[computeLoaders]).
     * [FOLIA] and [QUILT] need to be explicitly added via [extraLoaders].
     */
    @get:Input
    val apiTiers: SetProperty<String> = objects.setProperty(String::class.java)
    /**
     * Loaders to strip out of the tier-expanded set, for a project that's compatible with a tier in general but
     * not one specific descendant of it (e.g. `apiTiers(PAPER); excludeLoaders(PURPUR)`)
     */
    @get:Input
    val excludeLoaders: SetProperty<String> = objects.setProperty(String::class.java).convention(emptySet())
    /**
     * Loaders to add on top of the tier-expanded set that aren't implied by any tier — e.g. [FOLIA].
     * [FOLIA] is folded in automatically during [setup] when [MinecraftExtension.folia] is enabled, so both
     * [computeLoaders] and [ProjectData] see it consistently.
     */
    @get:Input
    val extraLoaders: SetProperty<String> = objects.setProperty(String::class.java).convention(emptySet())
    @get:Input
    val addResourceFile: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val dryRun: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    /**
     * Whether to embed `annoying-api` in the published artifact on Modrinth/CurseForge
     */
    @get:Input
    val addAnnoyingApiDependency: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    val projectData: PublishingPlatformsProjectDataExtension = objects.newInstance(
        PublishingPlatformsProjectDataExtension::class.java, project, this)

    var modPublishPlugin: (ModPublishExtension.() -> Unit)? = null
    var modrinth: (Modrinth.() -> Unit)? = null
    var curseforge: (Curseforge.() -> Unit)? = null
    val hangar: HangarExtension = HangarExtension()


    @Used
    fun projectData(id: String, action: PublishingPlatformsProjectDataExtension.() -> Unit = {}) {
        projectData.id = id
        projectData.action()
    }

    fun modPublishPlugin(action: ModPublishExtension.() -> Unit) {
        val before = modPublishPlugin
        modPublishPlugin = {
            before?.invoke(this)
            action()
        }
    }

    fun platform(platform: PluginPlatform, identifier: String) = platforms.put(platform, identifier)

    fun github(github: String) = platform(PluginPlatform.GITHUB, github)

    fun modrinth(modrinth: String, action: Modrinth.() -> Unit = {}) {
        platform(PluginPlatform.MODRINTH, modrinth)

        val before = this.modrinth
        this.modrinth = {
            before?.invoke(this)
            action()
        }
    }

    fun hangar(hangar: String, action: HangarExtension.() -> Unit = {}) {
        platform(PluginPlatform.HANGAR, hangar)
        this.hangar.apply(action)
    }

    fun spigot(spigot: String) = platform(PluginPlatform.SPIGOT, spigot)

    fun curseforge(curseforge: String, action: Curseforge.() -> Unit = {}) {
        platform(PluginPlatform.CURSEFORGE, curseforge)

        val before = this.curseforge
        this.curseforge = {
            before?.invoke(this)
            action()
        }
    }

    fun external(external: String) = platform(PluginPlatform.EXTERNAL, external)

    fun manual(manual: String) = platform(PluginPlatform.MANUAL, manual)

    /**
     * The effective, concrete loaders set for this project: every [apiTiers] entry expanded via
     * [tierExpansions] (an unrecognized tier just passes through as its own loader), plus [extraLoaders],
     * minus [excludeLoaders]. Used for the real `mod-publish-plugin` publish; [ProjectData] pushes the raw
     * [apiTiers]/[excludeLoaders]/[extraLoaders] instead of this, so `projects/data` can be expanded centrally.
     */
    internal fun computeLoaders(): Set<String> {
        val apiTiers = apiTiers.get()
        val extra = extraLoaders.get()
        val excluded = excludeLoaders.get()
        return (apiTiers + apiTiers.flatMap { tierExpansions[it].orEmpty() } + extra)
            .filterNot(excluded::contains)
            .toSet()
    }

    private fun resolveReleaseChannel(): ReleaseChannel = when {
        project.inGitHubPublish -> ReleaseChannel.RELEASE
        project.inGitHubPreRelease -> ReleaseChannel.BETA
        else -> ReleaseChannel.ALPHA
    }

    // File exists: file contents
    // In GitHub workflow:
    //   Non-STABLE: "github.com/REPO/commit/SHA"
    //   STABLE: release link
    // Else: "No changelog specified"
    private fun resolveChangelogText(releaseChannel: ReleaseChannel): String {
        val changelogFile = project.file("Changelogs/${project.version}.md")
        return when {
            // File
            changelogFile.exists() -> changelogFile.readText()

            project.inGitHubWorkflow -> run {
                val gitHubRepository =
                    project.getEnvironmentVariable("GITHUB_REPOSITORY") ?: return@run "No changelog specified"
                val githubLink = "https://github.com/${gitHubRepository}"

                // Non-STABLE: commit SHA
                if (releaseChannel != ReleaseChannel.RELEASE) return@run "${githubLink}/commit/${project.getEnvironmentVariable("GITHUB_SHA")}"

                // STABLE: release link
                "${githubLink}/releases/tag/${project.version}"
            }

            else -> "No changelog specified"
        }
    }

    internal fun setup(project: Project) {
        if (platforms.orNull?.isEmpty() == true) return

        // Add resource file task
        if (addResourceFile.get()) project.addPlatformsResourceFileTask(platforms.get())

        // Default apiTiers to Paper/Spigot dependency checking
        if (apiTiers.orNull.isNullOrEmpty()) when {
            minecraft.paper.version.orNull != null -> apiTiers.set(setOf(PAPER))
            minecraft.spigotAPI.version.orNull != null || minecraft.spigotNMS.version.orNull != null -> apiTiers.set(setOf(SPIGOT))
            else -> apiTiers.set(emptySet())
        }

        // Default minecraftVersionStart to Paper/Spigot Minecraft version
        if (minecraftVersionStart.orNull == null) minecraftVersionStart.set(minecraft.getMinecraftVersion())

        // Fold Folia into extraLoaders so both computeLoaders() and ProjectData see it
        if (minecraft.folia.get()) extraLoaders.set(extraLoaders.get() + FOLIA)

        // Setup project data publishing
        projectData.setup(project)

        val releaseChannel = resolveReleaseChannel()
        val primaryFile = project.tasks.named<Jar>(if (project.hasShadowPlugin()) "shadowJar" else "jar").flatMap { it.archiveFile }
        val changelogText = resolveChangelogText(releaseChannel)

        if (project.hasModPublishPlugin()) setupModPublish(project, releaseChannel, changelogText, primaryFile)

        if (project.hasHangarPublishPlugin()) setupHangar(project, releaseChannel, changelogText, primaryFile)
    }
}
