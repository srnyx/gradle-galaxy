package xyz.srnyx.gradlegalaxy.data.config.publishing

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing.HangarExtension
import xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing.PlatformPublishingExtension


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
    internal fun toExtension(): PlatformPublishingExtension.() -> Unit = {
        val config: PublishingPlatformConfig = this@PublishingPlatformConfig

        minecraftVersionStart.set(config.minecraftVersionStart)
        minecraftVersionEnd.set(config.minecraftVersionEnd)
        apiTiers.set(emptyList())
        extraLoaders.set(config.loaders)
        dryRun.set(config.dryRun)
        addAnnoyingApiDependency.set(config.addAnnoyingApiDependency)

        modPublishPlugin(action)
        config.platforms[PluginPlatform.MODRINTH]?.let { modrinth(it, modrinthAction) }
        config.platforms[PluginPlatform.CURSEFORGE]?.let { curseforge(it, curseForgeAction) }
        config.platforms[PluginPlatform.HANGAR]?.let { hangar(it, hangarAction) }
        config.platforms[PluginPlatform.SPIGOT]?.let { spigot(it) }
        config.platforms[PluginPlatform.EXTERNAL]?.let { external(it) }
        config.platforms[PluginPlatform.MANUAL]?.let { manual(it) }
    }
}
