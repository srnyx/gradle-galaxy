package xyz.srnyx.gradlegalaxy.utility

import java.lang.NumberFormatException


/**
 * A class to represent a semantic version (example: `1.19.2`)
 */
class SemanticVersion(version: String) : Comparable<SemanticVersion> {
    /**
     * The major version
     */
    val major: Int
    /**
     * The minor version
     */
    val minor: Int
    /**
     * The patch version
     */
    val patch: Int

    init {
        val versionSplit = version.split('.')
        require(versionSplit.size >= 2) { "Failed to parse Minecraft version (invalid version string): $version" }

        // Get patch
        try {
            major = versionSplit[0].toInt()
            minor = versionSplit[1].toInt()
            patch = versionSplit.getOrElse(2) { "0" }.toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Failed to parse Minecraft version (invalid values): $version")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SemanticVersion

        if (major != other.major) return false
        if (minor != other.minor) return false
        return patch == other.patch
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        return result
    }

    override fun toString(): String = "major.minor.patch"

    operator fun component1(): Int = major

    operator fun component2(): Int = minor

    operator fun component3(): Int = patch

    override operator fun compareTo(other: SemanticVersion): Int {
        if (major != other.minor) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }
}