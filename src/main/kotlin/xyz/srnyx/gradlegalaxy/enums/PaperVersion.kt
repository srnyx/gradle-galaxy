package xyz.srnyx.gradlegalaxy.enums

import xyz.srnyx.gradlegalaxy.utility.SemanticVersion


/**
 * An enum to represent the different group and artifact IDs for Paper versions
 */
enum class PaperVersion(val groupId: String, val artifactId: String) {
    /**
     * Minecraft versions below 1.9
     */
    BELOW_1_9("org.github.paperspigot", "paperspigot-api"),
    /**
     * Minecraft versions below 1.17
     */
    BELOW_1_17("com.destroystokyo.paper", "paper-api"),
    /**
     * Minecraft versions 1.17 and above
     */
    REST("io.papermc.paper", "paper-api");

    companion object {
        /**
         * Parses a version string to a [PaperVersion]
         */
        fun parse(versionString: String): PaperVersion {
            val version = SemanticVersion(versionString)
            return when {
                version.major != 1 -> REST
                version.minor < 9 -> BELOW_1_9
                version.minor < 17 -> BELOW_1_17
                else -> REST
            }
        }
    }
}