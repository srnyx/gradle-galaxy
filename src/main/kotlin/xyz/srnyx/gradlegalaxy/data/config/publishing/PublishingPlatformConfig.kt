package xyz.srnyx.gradlegalaxy.data.config.publishing

import io.papermc.hangarpublishplugin.HangarPublishExtension
import io.papermc.hangarpublishplugin.model.HangarPublication
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.networking.RequestContext.Default.json
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.ReleaseChannel
import xyz.srnyx.gradlegalaxy.utility.SemanticVersion
import xyz.srnyx.gradlegalaxy.utility.addPlatformsResourceFileTask
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.hasHangarPublishPlugin
import xyz.srnyx.gradlegalaxy.utility.hasModPublishPlugin
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.inGitHubPreRelease
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import xyz.srnyx.gradlegalaxy.utility.retrieveHangarPlatformVersions
import java.io.File
import javax.inject.Inject


data class PublishingPlatformConfig(
    val platforms: Map<PluginPlatform, String>,
    val minecraftVersionStart: String = "1.8.8",
    val minecraftVersionEnd: String? = null,
    val loaders: List<String> = listOf("spigot", "paper", "purpur"),
    val addAnnoyingApiDependency: Boolean = true,
    val dryRun: Boolean = false,
    val modrinthAction: Modrinth.() -> Unit = {},
    val curseForgeAction: Curseforge.() -> Unit = {},
    val hangarAction: HangarExtension.() -> Unit = {},
    val action: ModPublishExtension.() -> Unit = {},
) {
    internal fun toExtension(): PublishingPlatformExtension.() -> Unit = {
        val config: PublishingPlatformConfig = this@PublishingPlatformConfig

        minecraftVersionStart = config.minecraftVersionStart
        minecraftVersionEnd = config.minecraftVersionEnd
        loaders = config.loaders
        addAnnoyingApiDependency = config.addAnnoyingApiDependency
        dryRun = config.dryRun
        modrinth(modrinthAction)
        curseforge(curseForgeAction)
        hangar(hangarAction)
        modPublishPlugin(action)
    }
}

data class HangarDependency(
    val id: String,
    val required: Boolean,
)

abstract class HangarExtension @Inject constructor() {
    var dependencies: MutableList<HangarDependency> = mutableListOf()

    internal fun apply(hangar: HangarPublication) {
        hangar.platforms.paper {
            dependencies {
                this@HangarExtension.dependencies.forEach { dependency ->
                    hangar(dependency.id) { required.set(dependency.required) }
                }
            }
        }
    }

    fun optional(id: String) {
        dependencies.add(HangarDependency(id, false))
    }

    fun required(id: String) {
        dependencies.add(HangarDependency(id, true))
    }
}

abstract class PublishingPlatformExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var configured: Boolean = false

    lateinit var platforms: Map<PluginPlatform, String>
    var minecraftVersionStart: String = "1.8.8"
    var minecraftVersionEnd: String? = null
    var loaders: List<String> = listOf("spigot", "paper", "purpur")
    var addResourceFile: Boolean = true
    var addAnnoyingApiDependency: Boolean = true
    var dryRun: Boolean = false

    var modPublishPlugin: ModPublishExtension.() -> Unit = {}
    var modrinth: Modrinth.() -> Unit = {}
    var curseForge: Curseforge.() -> Unit = {}
    val hangar: HangarExtension = objects.newInstance(HangarExtension::class.java)

    fun modPublishPlugin(action: ModPublishExtension.() -> Unit) {
        val before = modPublishPlugin
        modPublishPlugin = {
            before()
            action()
        }
    }

    fun modrinth(action: Modrinth.() -> Unit) {
        val before = modrinth
        modrinth = {
            before()
            action()
        }
    }

    fun curseforge(action: Curseforge.() -> Unit) {
        val before = curseForge
        curseForge = {
            before()
            action()
        }
    }

    fun hangar(action: HangarExtension.() -> Unit) = hangar.action()

    internal fun setup(project: Project) {
        check(::platforms.isInitialized) { "Publishing platforms are not configured!" }
        if (platforms.isEmpty()) return
        
        // Add resource file task
        if (addResourceFile) project.addPlatformsResourceFileTask(platforms)

        // Setup publishing
        if (!project.hasModPublishPlugin()) return
        check(project.hasModPublishPlugin()) { "Mod Publish plugin is not applied!" }

        // Identifiers
        val modrinthIdentifier = platforms[PluginPlatform.MODRINTH]
        val curseForgeIdentifier = platforms[PluginPlatform.CURSEFORGE]
        val hangarIdentifier = platforms[PluginPlatform.HANGAR]

        // Release channel
        val releaseChannel: ReleaseChannel = when {
            inGitHubPublish -> ReleaseChannel.RELEASE
            inGitHubPreRelease -> ReleaseChannel.BETA
            else -> ReleaseChannel.ALPHA
        }

        // Primary file
        val primaryFile = project.tasks.named<Jar>(if (project.hasShadowPlugin()) "shadowJar" else "jar").flatMap { it.archiveFile }

        // Changelog
        // File exists: file contents
        // In GitHub workflow:
        //   Non-STABLE: "github.com/REPO/commit/SHA"
        //   STABLE: release link
        // Else: "No changelog specified"
        val changelogFile = project.file("Changelogs/${project.version}.md")
        val changelogText: String = when {
            // File
            changelogFile.exists() -> changelogFile.readText()

            inGitHubWorkflow -> run {
                val gitHubRepository =
                    getEnvironmentVariable("GITHUB_REPOSITORY") ?: return@run "No changelog specified"
                val githubLink = "https://github.com/${gitHubRepository}"

                // Non-STABLE: commit SHA
                if (releaseChannel != ReleaseChannel.RELEASE) return@run "${githubLink}/commit/${getEnvironmentVariable("GITHUB_SHA")}"

                // STABLE: release link
                "${githubLink}/releases/tag/${project.version}"
            }

            else -> "No changelog specified"
        }

        // Setup publishing
        project.extensions.configure<ModPublishExtension>("publishMods") {
            dryRun.set(this@PublishingPlatformExtension.dryRun)
            modLoaders.set(loaders)
            type.set(releaseChannel.mpp)
            changelog.set(changelogText)

            // Display name
            val event = getEnvironmentVariable("GITHUB_EVENT_PATH")
                ?.let { json.decodeFromString<JsonObject>(File(it).readText()) }
            displayName.set(
                event
                    // Release name
                    ?.get("release")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                // Commit name
                    ?: event?.get("commits")?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                        ?.lines()?.firstOrNull() // Only use commit title/summary, remove description
                    // Project version
                    ?: project.version.toString()
            )

            // Primary file (shadowJar or jar)
            file.set(primaryFile)

            // Additional files (javadocJar and sourcesJar)
            val javadocJarTask = project.tasks.findByName("javadocJar") as? Jar
            val sourcesJarTask = project.tasks.findByName("sourcesJar") as? Jar
            javadocJarTask?.let { additionalFiles.from(it) }
            sourcesJarTask?.let { additionalFiles.from(it) }

            val minecraftVersionEnd = minecraftVersionEnd ?: "latest"

            // Modrinth
            if (modrinthIdentifier != null) {
                val token = getEnvironmentVariable("MODRINTH_TOKEN")
                if (dryRun.get() || token != null) modrinth {
                    accessToken.set(token)
                    minecraftVersionRange {
                        start.set(minecraftVersionStart)
                        end.set(minecraftVersionEnd)
                    }

                    // Annoying API dependency
                    if (addAnnoyingApiDependency) embeds("annoying-api")

                    // Additional file types
                    javadocJarTask?.let { additionalFile(it.archiveFile) { type.set(JAVADOC_JAR) } }
                    sourcesJarTask?.let { additionalFile(it.archiveFile) { type.set(SOURCES_JAR) } }

                    projectId.set(modrinthIdentifier)
                    modrinth()
                }
            }

            // CurseForge
            if (curseForgeIdentifier != null) {
                val token = getEnvironmentVariable("CURSEFORGE_TOKEN")
                if (dryRun.get() || token != null) curseforge {
                    accessToken.set(getEnvironmentVariable("CURSEFORGE_TOKEN"))
                    minecraftVersionRange {
                        start.set(minecraftVersionStart)
                        end.set(minecraftVersionEnd)
                    }

                    if (addAnnoyingApiDependency) embeds("annoying-api")

                    projectId.set(curseForgeIdentifier)
                    curseForge()
                }
            }

            // Ensure publishing runs after building
            project.tasks.withType<PublishModTask> {
                dependsOn("jar")
                if (project.hasShadowPlugin()) dependsOn("shadowJar")
            }

            modPublishPlugin()
        }

        // Hangar Publish Plugin
        if (project.hasHangarPublishPlugin() && hangarIdentifier != null) {
            val token = getEnvironmentVariable("HANGAR_TOKEN")
            if (token != null) {
                project.extensions.configure<HangarPublishExtension>("hangarPublish") { publications.register("plugin") {
                    version.set(project.version.toString())
                    id.set(hangarIdentifier)
                    channel.set(releaseChannel.hangar)
                    changelog.set(changelogText)
                    apiKey.set(token)

                    platforms { paper {
                        jar.set(primaryFile)

                        // Get Hangar's supported Minecraft versions
                        val hangarMinecraftVersions = retrieveHangarPlatformVersions("PAPER")
                        if (!hangarMinecraftVersions.contains(minecraftVersionStart)) {
                            throw IllegalArgumentException("Hangar does not support start Minecraft version ${minecraftVersionStart}")
                        }

                        if (minecraftVersionEnd != null) {
                            // start -> end
                            if (!hangarMinecraftVersions.contains(minecraftVersionEnd)) {
                                throw IllegalArgumentException("Hangar does not support end Minecraft version ${minecraftVersionEnd}")
                            }
                            platformVersions.set(listOf(minecraftVersionStart + "-${minecraftVersionEnd}"))
                        } else {
                            // start -> latest
                            val semanticVersionStart = SemanticVersion(minecraftVersionStart)
                            platformVersions.set(hangarMinecraftVersions
                                .map { SemanticVersion(it) }
                                .filter { it >= semanticVersionStart }
                                .map { it.toString() })
                        }
                    } }

                    hangar.apply(this)
                } }

                // Ensure publishAllPublicationsToHangar runs with/after publishMods
                project.tasks.named("publishMods") { finalizedBy("publishAllPublicationsToHangar") }
            }
        }
    }
}
