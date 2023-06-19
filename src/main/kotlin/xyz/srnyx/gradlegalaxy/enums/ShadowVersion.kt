package xyz.srnyx.gradlegalaxy.enums

import xyz.srnyx.gradlegalaxy.utility.SemanticVersion


/**
 * An enum to represent the different group and artifact IDs for Shadow plugin versions
 */
enum class ShadowVersion(val groupId: String) {
    BELOW_7_0_0("com.github.jengelman.gradle.plugins"),
    BELOW_7_1_0("gradle.plugin.com.github.jengelman.gradle.plugins"),
    BELOW_8_1_0("gradle.plugin.com.github.johnrengelman"),
    REST("com.github.johnrengelman");

    companion object {
        /**
         * Parses a version string to a [ShadowVersion]
         */
        fun parse(versionString: String): ShadowVersion {
            val version = SemanticVersion(versionString)
            return when {
                version.major < 7 -> BELOW_7_0_0
                version.minor < 1 -> BELOW_7_1_0
                version.major < 8 -> BELOW_8_1_0
                else -> REST
            }
        }
    }
}
