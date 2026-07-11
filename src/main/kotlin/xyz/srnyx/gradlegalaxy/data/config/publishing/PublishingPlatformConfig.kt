package xyz.srnyx.gradlegalaxy.data.config.publishing

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import org.gradle.api.Action
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform


data class PublishingPlatformConfig(
    val platforms: Map<PluginPlatform, String>,
    val minecraftVersionStart: String = "1.8.8",
    val minecraftVersionEnd: String? = null,
    val loaders: List<String> = listOf("spigot", "paper", "purpur"),
    val addAnnoyingApiDependency: Boolean = true,
    val dryRun: Boolean = false,
    val modrinthAction: Action<Modrinth> = Action {},
    val curseForgeAction: Action<Curseforge> = Action {},
    val hangarAction: Action<HangarAction> = Action {},
    val action: Action<ModPublishExtension> = Action {},
)

class HangarAction {
    val dependencies: MutableList<HangarDependency> = mutableListOf()

    fun optional(id: String) {
        dependencies.add(HangarDependency(id, false))
    }

    fun required(id: String) {
        dependencies.add(HangarDependency(id, true))
    }
}

data class HangarDependency(
    val id: String,
    val required: Boolean,
)
