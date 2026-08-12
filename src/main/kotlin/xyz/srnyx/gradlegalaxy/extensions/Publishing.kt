package xyz.srnyx.gradlegalaxy.extensions

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
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import xyz.srnyx.gradlegalaxy.utility.retrieveHangarPlatformVersions
import xyz.srnyx.gradlegalaxy.utility.silenceMissingJavaDocWarnings
import java.io.File
import javax.inject.Inject


abstract class PublishingExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory
) {
    val simple = objects.newInstance(PublishingSimpleExtension::class.java)
    val env = objects.newInstance(PublishingEnvExtension::class.java)
    val platforms = objects.newInstance(PublishingPlatformExtension::class.java)

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
        deferred.defer(Phase.WIRE) { platforms.setup(project) }
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
        val resolvedMavenUrl = extension.mavenUrl.orNull ?: getEnvironmentVariable(extension.mavenUrlEnv.get())
        if (resolvedMavenUrl != null) project.getPublishing().repositories.maven {
            url = project.uri(resolvedMavenUrl)

            val usernameEnv = getEnvironmentVariable(extension.usernameEnv.get())
            val passwordEnv = getEnvironmentVariable(extension.passwordEnv.get())
            if (usernameEnv != null || passwordEnv != null) credentials {
                if (usernameEnv != null) username = usernameEnv
                if (passwordEnv != null) password = passwordEnv
            }
        }
    }
}

data class HangarDependency(
    val id: String,
    val required: Boolean,
)

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
    fun optional(id: String) {
        dependencies.add(HangarDependency(id, false))
    }

    @Used
    fun required(id: String) {
        dependencies.add(HangarDependency(id, true))
    }
}

abstract class UniversalDependency @Inject constructor(
    objects: ObjectFactory,
) {
    @get:Input
    val required: Property<Boolean> = objects.property(Boolean::class.java)
    @get:Input @get:Optional
    val modrinth: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val curseforge: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val hangar: Property<String> = objects.property(String::class.java)
}

abstract class PublishingPlatformExtension @Inject constructor(
    val objects: ObjectFactory,
) {
    @get:Input
    val platforms: MapProperty<PluginPlatform, String> = objects.mapProperty(PluginPlatform::class.java, String::class.java)
    @get:Input
    val minecraftVersionStart: Property<String> = objects.property(String::class.java).convention("1.8.8")
    @get:Input @get:Optional
    val minecraftVersionEnd: Property<String> = objects.property(String::class.java)
    @get:Input
    val loaders: ListProperty<String> = objects.listProperty(String::class.java).convention(listOf("spigot", "paper", "purpur"))
    @get:Input
    val addResourceFile: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input
    val addAnnoyingApiDependency: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    @get:Input @get:Optional
    val universalDependencies: ListProperty<UniversalDependency> = objects.listProperty(UniversalDependency::class.java)
    @get:Input
    val dryRun: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    var modPublishPlugin: ModPublishExtension.() -> Unit = {}
    var modrinth: Modrinth.() -> Unit = {}
    var curseforge: Curseforge.() -> Unit = {}
    val hangar: HangarExtension = HangarExtension()

    fun modPublishPlugin(action: ModPublishExtension.() -> Unit) {
        val before = modPublishPlugin
        modPublishPlugin = {
            before()
            action()
        }
    }

    fun platform(platform: PluginPlatform, identifier: String) {
        platforms.put(platform, identifier)
    }

    fun modrinth(modrinth: String, action: Modrinth.() -> Unit = {}) {
        platform(PluginPlatform.MODRINTH, modrinth)

        val before = this.modrinth
        this.modrinth = {
            before()
            action()
        }
    }

    fun hangar(hangar: String, action: HangarExtension.() -> Unit = {}) {
        platform(PluginPlatform.HANGAR, hangar)
        this.hangar.apply(action)
    }

    fun spigot(spigot: String) {
        platform(PluginPlatform.SPIGOT, spigot)
    }

    fun curseforge(curseforge: String, action: Curseforge.() -> Unit = {}) {
        platform(PluginPlatform.CURSEFORGE, curseforge)

        val before = this.curseforge
        this.curseforge = {
            before()
            action()
        }
    }

    fun external(external: String) {
        platform(PluginPlatform.EXTERNAL, external)
    }

    fun manual(manual: String) {
        platform(PluginPlatform.MANUAL, manual)
    }

    //TODO maybe move optional and required to nested "dependencies" (or similar) extension
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

    internal fun setup(project: Project) {
        if (platforms.orNull?.isEmpty() == true) return

        // Add resource file task
        if (addResourceFile.get()) project.addPlatformsResourceFileTask(platforms.get())

        // Setup publishing
        if (!project.hasModPublishPlugin()) return
        check(project.hasModPublishPlugin()) { "Mod Publish plugin is not applied!" }

        // Identifiers
        val modrinthIdentifier = platforms.get()[PluginPlatform.MODRINTH]
        val curseForgeIdentifier = platforms.get()[PluginPlatform.CURSEFORGE]
        val hangarIdentifier = platforms.get()[PluginPlatform.HANGAR]

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

            val minecraftVersionEnd = minecraftVersionEnd.getOrElse("latest")

            // Modrinth
            if (modrinthIdentifier != null) {
                val token = getEnvironmentVariable("MODRINTH_TOKEN")
                if (dryRun.get() || token != null) modrinth {
                    accessToken.set(token)
                    minecraftVersionRange {
                        start.set(minecraftVersionStart.get())
                        end.set(minecraftVersionEnd)
                    }

                    // Annoying API dependency
                    if (addAnnoyingApiDependency.get()) embeds("annoying-api")

                    // Universal dependencies
                    universalDependencies.orNull?.forEach { dependency ->
                        dependency.modrinth.orNull?.let {
                            if (dependency.required.get()) requires(it) else optional(it)
                        }
                    }

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
                        start.set(minecraftVersionStart.get())
                        end.set(minecraftVersionEnd)
                    }

                    // Annoying API dependency
                    if (addAnnoyingApiDependency.get()) embeds("annoying-api")

                    // Universal dependencies
                    universalDependencies.orNull?.forEach { dependency ->
                        dependency.curseforge.orNull?.let {
                            if (dependency.required.get()) requires(it) else optional(it)
                        }
                    }

                    projectId.set(curseForgeIdentifier)
                    curseforge()
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
                    universalDependencies.orNull?.forEach { dependency ->
                        dependency.hangar.orNull?.let {
                            hangar.dependencies += HangarDependency(it, dependency.required.get())
                        }
                    }

                    // Custom action
                    hangar.apply(this)
                } }

                // Ensure publishAllPublicationsToHangar runs with/after publishMods
                project.tasks.named("publishMods") { finalizedBy("publishAllPublicationsToHangar") }
            }
        }
    }
}
