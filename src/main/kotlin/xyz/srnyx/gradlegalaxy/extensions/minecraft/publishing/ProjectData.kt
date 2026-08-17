package xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.inGitHubRelease
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject


abstract class PublishingPlatformsProjectDataExtension @Inject constructor(
    project: Project,
    private val platformPublishing: PlatformPublishingExtension,
    objects: ObjectFactory,
) {
    lateinit var id: String

    @get:Input
    val url: Property<String> = objects.property(String::class.java).convention("https://srnyx.com/projects/data/")
    @get:Input
    val token: Property<String> = objects.property(String::class.java).convention(project.getEnvironmentVariable("PUBLISHING_PROJECT_DATA_TOKEN"))

    fun setup(project: Project) {
        if (!::id.isInitialized) return

        // Add ID to URL
        if (!url.get().endsWith("/")) url.set("${url.get()}/")
        url.set("${url.get()}$id")

        val minecraftVersions = when {
            platformPublishing.minecraftVersionStart.isPresent && platformPublishing.minecraftVersionEnd.isPresent ->
                "${platformPublishing.minecraftVersionStart.get()}-${platformPublishing.minecraftVersionEnd.get()}"
            platformPublishing.minecraftVersionStart.isPresent -> "${platformPublishing.minecraftVersionStart.get()}+"
            platformPublishing.minecraftVersionEnd.isPresent -> "${platformPublishing.minecraftVersionEnd.get()}-"
            else -> throw IllegalArgumentException("Must specify at least one of minecraftVersionStart or minecraftVersionEnd")
        }

        val data = ProjectData(
            platforms = platformPublishing.platforms.get(),
            apiTiers = platformPublishing.apiTiers.get(),
            excludeLoaders = platformPublishing.excludeLoaders.get(),
            extraLoaders = platformPublishing.extraLoaders.get(),
            minecraftVersions = listOf(minecraftVersions))

        val urlString = url.get()
        val tokenString = token.orNull
        val jsonData = Json.encodeToString(ProjectData.serializer(), data)

        val publishProjectData = project.tasks.register("publishProjectData") {
            group = "galaxy"
            description = "Publishes the project data to $urlString"

            doLast {
                // Dry run (print to console)
                if (platformPublishing.dryRun.get() || tokenString == null) {
                    logger.lifecycle("Dry run: $jsonData")
                    return@doLast
                }

                try {
                    val connection = URL(urlString).openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $tokenString")
                    connection.doOutput = true

                    connection.outputStream.use { os ->
                        os.write(jsonData.toByteArray())
                        os.flush()
                    }

                    val code = connection.responseCode
                    if (code !in 200..299) logger.warn("Failed to publish project data: $code ${connection.responseMessage}")
                } catch (e: Exception) {
                    logger.warn("Failed to publish project data: ${e.message}")
                }
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
    val apiTiers: Set<String>,
    @SerialName("exclude-loaders")
    val excludeLoaders: Set<String>,
    @SerialName("extra-loaders")
    val extraLoaders: Set<String>,
    /**
     * Supports `1.8.8+` (greater than or equal), `1.21.11-` (less than or equal), `1.13-1.21.11` (range)
     */
    @SerialName("minecraft-versions")
    val minecraftVersions: List<String>,
)
