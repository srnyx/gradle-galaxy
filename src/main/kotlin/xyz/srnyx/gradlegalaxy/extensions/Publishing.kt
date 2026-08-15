package xyz.srnyx.gradlegalaxy.extensions

import io.papermc.hangarpublishplugin.HangarPublishExtension
import io.papermc.hangarpublishplugin.model.HangarPublication
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.config.publishing.TextArtifact
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.data.pom.LicenseData
import xyz.srnyx.gradlegalaxy.data.pom.ScmData
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.ReleaseChannel
import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.PluginYmlExtension
import xyz.srnyx.gradlegalaxy.utility.SemanticVersion
import xyz.srnyx.gradlegalaxy.utility.addJavadocSourcesJars
import xyz.srnyx.gradlegalaxy.utility.addPlatformsResourceFileTask
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.getPublishing
import xyz.srnyx.gradlegalaxy.utility.hasHangarPublishPlugin
import xyz.srnyx.gradlegalaxy.utility.hasModPublishPlugin
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.inGitHubPreRelease
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubRelease
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import xyz.srnyx.gradlegalaxy.utility.retrieveHangarPlatformVersions
import xyz.srnyx.gradlegalaxy.utility.silenceMissingJavaDocWarnings
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject


abstract class PublishingExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    private val minecraft: MinecraftExtension,
    objects: ObjectFactory
) {
    val simple = objects.newInstance(PublishingSimpleExtension::class.java)
    val env = objects.newInstance(PublishingEnvExtension::class.java)
    val platforms = PublishingPlatformExtension(project, objects)

    fun simple(action: PublishingSimpleExtension.() -> Unit = {}) {
        simple.action()
        deferred.defer(Phase.WIRE) { simple.setup(project) }
    }
    fun env(action: PublishingEnvExtension.() -> Unit = {}) {
        env.action()
        deferred.defer(Phase.WIRE) { env.setup(project) }
    }
    fun platforms(action: PublishingPlatformExtension.() -> Unit = {}) {
        platforms.action()
        deferred.defer(Phase.WIRE) { platforms.setup(project, minecraft) }
    }
}

abstract class PublishingSimpleExtension @Inject constructor(
    objects: ObjectFactory
) {
    @get:Input @get:Optional
    val groupId: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val artifactId: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val version: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val withJavadocSourcesJars: Property<Boolean> = objects.property<Boolean>(Boolean::class.java).convention(true)
    @get:Input @get:Optional
    val silenceMissingJavadocWarnings: Property<Boolean> = objects.property<Boolean>(Boolean::class.java).convention(false)
    @get:Input @get:Optional
    val component: Property<SoftwareComponent> = objects.property(SoftwareComponent::class.java)
    @get:Input @get:Optional
    val artifacts: ListProperty<Any> = objects.listProperty(Any::class.java)
    @get:Input @get:Optional
    val textArtifacts: ListProperty<TextArtifact> = objects.listProperty(TextArtifact::class.java)
    @get:Input @get:Optional
    val licenses: ListProperty<LicenseData> = objects.listProperty(LicenseData::class.java)
    @get:Input @get:Optional
    val developers: ListProperty<DeveloperData> = objects.listProperty(DeveloperData::class.java)
    @get:Input @get:Optional
    val scm: Property<ScmData> = objects.property(ScmData::class.java)

    var publicationAction: MavenPublication.() -> Unit = {}

    fun publication(action: MavenPublication.() -> Unit) {
        val previous = publicationAction
        publicationAction = {
            previous(this)
            action(this)
        }
    }

    internal fun setup(project: Project) {
        val extension: PublishingSimpleExtension = this@PublishingSimpleExtension

        project.apply(plugin = "maven-publish")

        // Javadocs and sources
        if (withJavadocSourcesJars.get()) project.addJavadocSourcesJars()

        // Silence missing Javadoc warnings
        if (silenceMissingJavadocWarnings.get()) project.silenceMissingJavaDocWarnings()

        // Create publication
        project.getPublishing().publications.create<MavenPublication>("maven") {
            extension.groupId.orNull?.let { this.groupId = it }
            extension.artifactId.orNull?.let { this.artifactId = it }
            extension.version.orNull?.let { this.version = it }

            from(extension.component.getOrElse(project.components["java"]))

            extension.artifacts.orNull?.forEach(this::artifact)
            textArtifacts.orNull?.forEach { textArtifact ->
                val taskName = "generate${textArtifact.classifier.capitalized()}TextArtifact"
                val extensionSuffix = textArtifact.extension?.let { ".$it" } ?: ""
                val outputFile = project.layout.buildDirectory.file("generated/publications/${this.artifactId}-${this.version}-${textArtifact.classifier}$extensionSuffix")

                val textProvider = project.provider { textArtifact.text.invoke() }
                val task = project.tasks.register(taskName) {
                    group = "publishing"
                    description = "Generates the ${textArtifact.classifier} artifact for publication ${this.name}"

                    inputs.property("text", textProvider)
                    outputs.file(outputFile)

                    doLast {
                        outputFile.get().asFile.apply {
                            parentFile.mkdirs()
                            writeText(textProvider.get())
                        }
                    }
                }

                artifact(outputFile) {
                    this.classifier = textArtifact.classifier
                    this.extension = textArtifact.extension
                    builtBy(task)
                }
            }
            pom {
                name.set(project.name)
                description.set(project.description)

                licenses { extension.licenses.orNull?.forEach { license {
                    this.name.set(it.name)
                    this.url.set(it.url)
                    it.distribution?.value?.let(this.distribution::set)
                    it.comments?.let(this.comments::set)
                } } }

                developers { extension.developers.orNull
                    ?.filterNot(DeveloperData::isEmpty)
                    ?.forEach { developer {
                        it.id?.let(this.id::set)
                        it.name?.let(this.name::set)
                        it.url?.let(this.url::set)
                        it.email?.let(this.email::set)
                        it.timezone?.let(this.timezone::set)
                        it.organization?.let(this.organization::set)
                        it.organizationUrl?.let(this.organizationUrl::set)
                        it.roles.takeIf(List<String>::isNotEmpty)?.let(this.roles::set)
                        it.properties.takeIf(Map<String, String>::isNotEmpty)?.let(this.properties::set)
                    } } }

                extension.scm.orNull?.let { scm -> scm {
                    connection.set(scm.connection)
                    developerConnection.set(scm.developerConnection)
                    scm.url?.let(this.url::set)
                    scm.tag?.let(this.tag::set)
                } }
            }

            publicationAction(this)
        }
    }
}

abstract class PublishingEnvExtension @Inject constructor(
    objects: ObjectFactory
) {
    @get:Input @get:Optional
    val mavenUrlEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_URL")
    @get:Input @get:Optional
    val usernameEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_NAME")
    @get:Input @get:Optional
    val passwordEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_SECRET")
    @get:Input @get:Optional
    val mavenUrl: Property<String> = objects.property(String::class.java)

    internal fun setup(project: Project) {
        val extension: PublishingEnvExtension = this@PublishingEnvExtension

        project.apply(plugin = "maven-publish")

        // Create repository
        val resolvedMavenUrl = extension.mavenUrl.orNull ?: project.getEnvironmentVariable(extension.mavenUrlEnv.get())
        if (resolvedMavenUrl != null) project.getPublishing().repositories.maven {
            url = project.uri(resolvedMavenUrl)

            val usernameEnv = project.getEnvironmentVariable(extension.usernameEnv.get())
            val passwordEnv = project.getEnvironmentVariable(extension.passwordEnv.get())
            if (usernameEnv != null || passwordEnv != null) credentials {
                if (usernameEnv != null) username = usernameEnv
                if (passwordEnv != null) password = passwordEnv
            }
        }
    }
}

class PublishingPlatformExtension(
    project: Project,
    objects: ObjectFactory
) {
    /**
     * Needs explicit addition
     */
    val FOLIA = "folia"
    val PURPUR = "purpur"
    val PAPER = "paper"
    val SPIGOT = "spigot"
    val BUKKIT = "bukkit"
    val FABRIC = "fabric"
    /**
     * Needs explicit addition
     */
    val QUILT = "quilt"
    val FORGE = "forge"
    val NEOFORGE = "neoforge"

    /**
     * Local, gradle-galaxy-side tier -> concrete-loaders expansion,
     * used only to compute *this release's* `mod-publish-plugin` loaders list.
     */
    private val tierExpansions: Map<String, List<String>> = mapOf(
        BUKKIT to listOf(SPIGOT, PAPER, PURPUR),
        SPIGOT to listOf(PAPER, PURPUR),
        PAPER to listOf(PURPUR))

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
    val apiTiers: ListProperty<String> = objects.listProperty(String::class.java).convention(listOf(SPIGOT))
    /**
     * Loaders to strip out of the tier-expanded set, for a project that's compatible with a tier in general but
     * not one specific descendant of it (e.g. `apiTiers(PAPER); excludeLoaders(PURPUR)`)
     */
    @get:Input
    val excludeLoaders: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    /**
     * Loaders to add on top of the tier-expanded set that aren't implied by any tier — e.g. [FOLIA]
     */
    @get:Input
    val extraLoaders: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    @get:Input
    val addResourceFile: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val dryRun: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val dependency: PublishingPlatformsDependencyExtension = objects.newInstance(PublishingPlatformsDependencyExtension::class.java)
    val projectData: PublishingPlatformsProjectDataExtension = objects.newInstance(PublishingPlatformsProjectDataExtension::class.java, project, this)

    var modPublishPlugin: (ModPublishExtension.() -> Unit)? = null
    var modrinth: (Modrinth.() -> Unit)? = null
    var curseforge: (Curseforge.() -> Unit)? = null
    val hangar: HangarExtension = HangarExtension()


    fun dependency(action: PublishingPlatformsDependencyExtension.() -> Unit) = dependency.action()
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
     * The effective, concrete loaders list for this project: every [apiTiers] entry expanded via
     * [tierExpansions] (an unrecognized tier just passes through as its own loader), plus [extraLoaders],
     * minus [excludeLoaders]. Used for the real `mod-publish-plugin` publish; [ProjectData] pushes the raw
     * [apiTiers]/[excludeLoaders]/[extraLoaders] instead of this, so `projects/data` can be expanded centrally.
     */
    internal fun computeLoaders(): List<String> {
        val apiTiers = apiTiers.get()
        val excluded = excludeLoaders.get().toSet()
        return (apiTiers + apiTiers.flatMap { tierExpansions[it].orEmpty() } + extraLoaders.get())
            .distinct()
            .filterNot(excluded::contains)
    }

    internal fun setup(
        project: Project,
        minecraft: MinecraftExtension,
    ) {
        // Universal dependencies pluginYml
        dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addPluginYml(minecraft.pluginYml) }

        // Don't do anything else if there aren't any platforms defined
        if (platforms.orNull?.isEmpty() == true) return

        // Add resource file task
        if (addResourceFile.get()) project.addPlatformsResourceFileTask(platforms.get())

        if (!project.hasModPublishPlugin()) return

        // Default minecraftVersionStart to Paper/Spigot Minecraft version
        if (minecraftVersionStart.orNull == null) minecraftVersionStart.set(minecraft.getMinecraftVersion())

        // Setup project data publishing
        projectData.setup(project)

        // Identifiers
        val modrinthIdentifier = platforms.get()[PluginPlatform.MODRINTH]
        val curseForgeIdentifier = platforms.get()[PluginPlatform.CURSEFORGE]
        val hangarIdentifier = platforms.get()[PluginPlatform.HANGAR]

        // Release channel
        val releaseChannel: ReleaseChannel = when {
            project.inGitHubPublish -> ReleaseChannel.RELEASE
            project.inGitHubPreRelease -> ReleaseChannel.BETA
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

        // Setup publishing
        project.extensions.configure<ModPublishExtension>("publishMods") {
            dryRun.set(this@PublishingPlatformExtension.dryRun)
            modLoaders.set(computeLoaders())
            type.set(releaseChannel.mpp)
            changelog.set(changelogText)

            // Display name
            val event = project.getEnvironmentVariable("GITHUB_EVENT_PATH")
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

            val minecraftVersionEnd = minecraftVersionEnd.getOrElse("latest")

            // Modrinth
            if (modrinthIdentifier != null) {
                val token = project.getEnvironmentVariable("MODRINTH_TOKEN")
                if (dryRun.get() || token != null) modrinth {
                    accessToken.set(token)
                    minecraftVersionRange {
                        start.set(minecraftVersionStart.get())
                        end.set(minecraftVersionEnd)
                    }

                    // Annoying API dependency
                    if (dependency.addAnnoyingApiDependency.get()) embeds("annoying-api")

                    // Universal dependencies
                    dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addModrinth(this) }

                    // Additional file types
                    javadocJarTask?.let { additionalFile(it.archiveFile) { type.set(JAVADOC_JAR) } }
                    sourcesJarTask?.let { additionalFile(it.archiveFile) { type.set(SOURCES_JAR) } }

                    projectId.set(modrinthIdentifier)
                    this@PublishingPlatformExtension.modrinth?.invoke(this)
                }
            }

            // CurseForge
            if (curseForgeIdentifier != null) {
                val token = project.getEnvironmentVariable("CURSEFORGE_TOKEN")
                if (dryRun.get() || token != null) curseforge {
                    accessToken.set(project.getEnvironmentVariable("CURSEFORGE_TOKEN"))
                    minecraftVersionRange {
                        start.set(minecraftVersionStart.get())
                        end.set(minecraftVersionEnd)
                    }

                    // Annoying API dependency
                    if (dependency.addAnnoyingApiDependency.get()) embeds("annoying-api")

                    // Universal dependencies
                    dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addCurseforge(this) }

                    projectId.set(curseForgeIdentifier)
                    this@PublishingPlatformExtension.curseforge?.invoke(this)
                }
            }

            // Ensure publishing runs after building
            project.tasks.withType<PublishModTask> {
                dependsOn("jar")
                if (project.hasShadowPlugin()) dependsOn("shadowJar")
            }

            this@PublishingPlatformExtension.modPublishPlugin?.invoke(this)
        }

        // Hangar Publish Plugin
        if (project.hasHangarPublishPlugin() && hangarIdentifier != null) {
            val token = project.getEnvironmentVariable("HANGAR_TOKEN")
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
                        if (!hangarMinecraftVersions.contains(minecraftVersionStart.get())) {
                            throw IllegalArgumentException("Hangar does not support start Minecraft version ${minecraftVersionStart.get()}")
                        }

                        if (minecraftVersionEnd.orNull != null) {
                            // start -> end
                            if (!hangarMinecraftVersions.contains(minecraftVersionEnd.get())) {
                                throw IllegalArgumentException("Hangar does not support end Minecraft version ${minecraftVersionEnd.get()}")
                            }
                            platformVersions.set(listOf(minecraftVersionStart.get() + "-${minecraftVersionEnd.get()}"))
                        } else {
                            // start -> latest
                            val semanticVersionStart = SemanticVersion(minecraftVersionStart.get())
                            platformVersions.set(hangarMinecraftVersions
                                .map { SemanticVersion(it) }
                                .filter { it >= semanticVersionStart }
                                .map { it.toString() })
                        }
                    } }

                    // Universal dependencies (add to custom action)
                    dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addHangar(hangar) }

                    // Custom action
                    hangar.apply(this)
                } }

                // Ensure publishAllPublicationsToHangar runs with/after publishMods
                project.tasks.named("publishMods") { finalizedBy("publishAllPublicationsToHangar") }
            }
        }
    }
}

abstract class PublishingPlatformsDependencyExtension @Inject constructor(
    private val objects: ObjectFactory,
) {
    @get:Input
    val addAnnoyingApiDependency: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input @get:Optional
    val universalDependencies: ListProperty<UniversalDependency> = objects.listProperty(UniversalDependency::class.java)

    @Used
    fun optional(action: UniversalDependency.() -> Unit) = universalDependency(false, action)

    @Used
    fun required(action: UniversalDependency.() -> Unit) = universalDependency(true, action)

    private fun universalDependency(required: Boolean, action: UniversalDependency.() -> Unit) {
        val dependency = objects.newInstance(UniversalDependency::class.java)
        dependency.required.set(required)
        dependency.action()
        universalDependencies.add(dependency)
    }
}

class HangarExtension {
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

    @Used
    fun optional(id: String) = dependencies.add(HangarDependency(id, false))

    @Used
    fun required(id: String) = dependencies.add(HangarDependency(id, true))
}

data class HangarDependency(
    val id: String,
    val required: Boolean,
)

abstract class UniversalDependency @Inject constructor(
    objects: ObjectFactory,
) {
    @get:Input
    val required: Property<Boolean> = objects.property(Boolean::class.java)
    @get:Input @get:Optional
    val pluginYml: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val modrinth: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val curseforge: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val hangar: Property<String> = objects.property(String::class.java)


    internal fun addPluginYml(pluginYmlExtension: PluginYmlExtension) {
        if (pluginYml.orNull == null) return
        pluginYmlExtension.apply { (if (required.get()) depend else softDepend).add(pluginYml.get()) }
    }

    internal fun addModrinth(modrinthTask: Modrinth) {
        if (modrinth.orNull == null) return
        modrinthTask.apply { if (required.get()) requires(modrinth.get()) else optional(modrinth.get()) }
    }

    internal fun addCurseforge(curseforgeTask: Curseforge) {
        if (curseforge.orNull == null) return
        curseforgeTask.apply { if (required.get()) requires(curseforge.get()) else optional(curseforge.get()) }
    }

    internal fun addHangar(hangarExtension: HangarExtension) {
        if (hangar.orNull == null) return
        hangarExtension.dependencies += HangarDependency(hangar.get(), required.get())
    }
}

abstract class PublishingPlatformsProjectDataExtension @Inject constructor(
    project: Project,
    private val publishingPlatforms: PublishingPlatformExtension,
    objects: ObjectFactory,
) {
    lateinit var id: String

    @get:Input
    val url: Property<String> = objects.property(String::class.java).convention("https://srnyx.com/projects/data")
    @get:Input
    val token: Property<String> = objects.property(String::class.java).convention(project.getEnvironmentVariable("PUBLISHING_PROJECT_DATA_TOKEN"))

    fun setup(project: Project) {
        if (token.orNull == null) return

        val minecraftVersions = when {
            publishingPlatforms.minecraftVersionStart.isPresent && publishingPlatforms.minecraftVersionEnd.isPresent ->
                "${publishingPlatforms.minecraftVersionStart.get()}-${publishingPlatforms.minecraftVersionEnd.get()}"
            publishingPlatforms.minecraftVersionStart.isPresent -> "${publishingPlatforms.minecraftVersionStart.get()}+"
            publishingPlatforms.minecraftVersionEnd.isPresent -> "${publishingPlatforms.minecraftVersionEnd.get()}-"
            else -> throw IllegalArgumentException("Must specify at least one of minecraftVersionStart or minecraftVersionEnd")
        }

        val data = ProjectData(
            platforms = publishingPlatforms.platforms.get(),
            apiTiers = publishingPlatforms.apiTiers.get(),
            excludeLoaders = publishingPlatforms.excludeLoaders.get(),
            extraLoaders = publishingPlatforms.extraLoaders.get(),
            minecraftVersions = listOf(minecraftVersions))

        val publishProjectData = project.tasks.register("publishProjectData") {
            group = "publishing"
            description = "Publishes the project data to ${url.get()}"

            doLast {
                val jsonData = Json.encodeToString(ProjectData.serializer(), data)
                val connection = URL(url.get()).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${token.get()}")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(jsonData.toByteArray())
                    os.flush()
                }

                if (connection.responseCode != 200) throw RuntimeException("Failed to publish project data: ${connection.responseMessage}")
            }
        }

        // Only publish project data if running in a GitHub Release
        if (project.inGitHubRelease) project.tasks.named("publishMods") { finalizedBy(publishProjectData) }
    }
}

@Serializable
data class ProjectData(
    @SerialName("platforms")
    val platforms: Map<PluginPlatform, String>,
    @SerialName("api-tiers")
    val apiTiers: List<String>,
    @SerialName("exclude-loaders")
    val excludeLoaders: List<String>,
    @SerialName("extra-loaders")
    val extraLoaders: List<String>,
    /**
     * Supports `1.8.8+` (greater than or equal), `1.21.11-` (less than or equal), `1.13-1.21.11` (range)
     */
    @SerialName("minecraft-versions")
    val minecraftVersions: List<String>,
)
