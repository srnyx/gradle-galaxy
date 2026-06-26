package xyz.srnyx.gradlegalaxy.data.config.publishing

import xyz.srnyx.gradlegalaxy.data.platforms.PluginPlatform


data class PublishingPlatformConfig(
    val platforms: Map<PluginPlatform, String>,
    val minecraftVersionStart: String = "1.8.8",
    val minecraftVersionEnd: String? = null,
    val loaders: List<String> = listOf("spigot", "paper", "purpur"),
)
