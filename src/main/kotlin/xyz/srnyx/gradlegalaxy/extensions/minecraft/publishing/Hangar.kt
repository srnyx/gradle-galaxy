package xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing

import io.papermc.hangarpublishplugin.HangarPublishExtension
import io.papermc.hangarpublishplugin.HangarPublishTask
import io.papermc.hangarpublishplugin.model.HangarPublication
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.util.internal.VersionNumber
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.ReleaseChannel
import org.gradle.kotlin.dsl.withType
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


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

internal fun PlatformPublishingExtension.setupHangar(
    project: Project,
    releaseChannel: ReleaseChannel,
    changelogText: String,
    primaryFile: Provider<RegularFile>,
) {
    val hangarIdentifier = platforms.get()[PluginPlatform.HANGAR] ?: return
    val token = project.getEnvironmentVariable("HANGAR_TOKEN") ?: return

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
                val semanticVersionStart = VersionNumber.parse(minecraftVersionStart.get())
                platformVersions.set(hangarMinecraftVersions
                    .map { VersionNumber.parse(it) }
                    .filter { it >= semanticVersionStart }
                    .map { it.toString() })
            }
        } }

        // Universal dependencies (add to custom action)
        minecraft.dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addHangar(hangar) }

        // Custom action
        hangar.apply(this)
    } }

    // Ensure publishing runs after building
    project.tasks.withType<HangarPublishTask> {
        dependsOn("jar")
        if (project.hasShadowPlugin()) dependsOn("shadowJar")
    }

    // Ensure publishAllPublicationsToHangar runs with/after publishMods
    project.tasks.named("publishMods") { finalizedBy("publishAllPublicationsToHangar") }
}

/**
 * Retrieve the versions of the specified platform from Hangar
 *
 * @param platform The platform to retrieve versions for (default: `PAPER`)
 *
 * @return The versions of the specified platform in a [LinkedHashSet] sorted by version (highest to lowest)
 */
private fun retrieveHangarPlatformVersions(platform: String = "PAPER"): LinkedHashSet<String> {
    // Make API request
    val response = HttpClient.newBuilder().build().send(
        HttpRequest.newBuilder()
            .uri(URI.create("https://hangar.papermc.io/api/v1/platforms/${platform}/versions"))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() != 200) {
        throw IllegalStateException("Failed to retrieve Hangar platform versions for $platform: ${response.statusCode()} ${response.body()}")
    }

    // Flatten versions
    val versions = LinkedHashSet<String>()
    for (element in json.decodeFromString<JsonArray>(response.body())) {
        val jsonObject = element.jsonObject
        // Add subVersions first as version is lowest
        for (subVersion in jsonObject["subVersions"]!!.jsonArray) versions.add(subVersion.jsonPrimitive.content)
        versions.add(jsonObject["version"]!!.jsonPrimitive.content)
    }
    return versions
}
