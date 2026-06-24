package xyz.srnyx.gradlegalaxy.data.config.dependency


/**
 * Configuration for Spigot-related dependencies
 *
 * @param setJavaVersion Whether to set the Java version when using this Spigot-related dependency (default: true)
 */
data class SpigotConfig(
    val setJavaVersion: Boolean = true,
)
