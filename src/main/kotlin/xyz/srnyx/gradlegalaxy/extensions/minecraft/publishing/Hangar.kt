package xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing

import io.papermc.hangarpublishplugin.HangarPublishExtension
import io.papermc.hangarpublishplugin.model.HangarPublication
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.util.internal.VersionNumber
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.ReleaseChannel
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.retrieveHangarPlatformVersions


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

    // Ensure publishAllPublicationsToHangar runs with/after publishMods
    project.tasks.named("publishMods") { finalizedBy("publishAllPublicationsToHangar") }
}
