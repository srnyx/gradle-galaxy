package xyz.srnyx.gradlegalaxy.data

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.kotlin.dsl.exclude

import xyz.srnyx.gradlegalaxy.enums.AdventureComponent
import xyz.srnyx.gradlegalaxy.enums.Component


/**
 * Represents a dependency on a component of Adventure
 *
 * @param component The component to depend on
 * @param version The version of the component to depend on
 * @param configuration The configuration to add the dependency to
 * @param configurationAction The action to apply to the dependency
 */
data class AdventureDependency(val component: Component, val version: String, val configuration: String? = null, val configurationAction: ExternalModuleDependency.() -> Unit = {}) {
    companion object {
        /**
         * Gets the default Adventure dependencies for Annoying API (Spigot)
         *
         * @param configuration The configuration to add the dependencies to
         * @param apiVersion The version of [AdventureComponent.API] to depend on
         * @param textMinimessageVersion The version of [AdventureComponent.Text.MINIMESSAGE] to depend on
         * @param textSerializerLegacyVersion The version of [AdventureComponent.Text.Serializer.LEGACY] to depend on
         * @param textSerializerPlainVersion The version of [AdventureComponent.Text.Serializer.PLAIN] to depend on
         * @param platformBukkitVersion The version of [AdventureComponent.Platform.BUKKIT] to depend on
         * @param textLoggerSlf4j The version of [AdventureComponent.Text.Logger.SLF4J] to depend on
         *
         * @return The default Adventure dependencies for Annoying API (Spigot)
         */
        fun getDefaultAnnoyingSpigot(
            configuration: String = "implementation",
            apiVersion: String = "4.14.0",
            textMinimessageVersion: String = "4.14.0",
            textSerializerLegacyVersion: String = "4.14.0",
            textSerializerPlainVersion: String = "4.14.0",
            platformBukkitVersion: String = "4.3.0",
            textLoggerSlf4j: String = "4.14.0",
        ): Array<AdventureDependency> = arrayOf(
            *getDefaultAnnoyingPaper(configuration, textSerializerLegacyVersion, platformBukkitVersion),
            AdventureDependency(AdventureComponent.API, apiVersion, configuration),
            AdventureDependency(AdventureComponent.Text.MINIMESSAGE, textMinimessageVersion, configuration),
            AdventureDependency(AdventureComponent.Text.Serializer.PLAIN, textSerializerPlainVersion, configuration),
            AdventureDependency(AdventureComponent.Text.Logger.SLF4J, textLoggerSlf4j, configuration))

        /**
         * Gets the default Adventure dependencies for Annoying API (Paper)
         *
         * @param configuration The configuration to add the dependencies to
         * @param textSerializerLegacyVersion The version of [AdventureComponent.Text.Serializer.LEGACY] to depend on
         * @param platformBukkitVersion The version of [AdventureComponent.Platform.BUKKIT] to depend on
         *
         * @return The default Adventure dependencies for Annoying API (Paper)
         */
        fun getDefaultAnnoyingPaper(
            configuration: String = "implementation",
            textSerializerLegacyVersion: String = "4.14.0",
            platformBukkitVersion: String = "4.3.0",
        ): Array<AdventureDependency> = arrayOf(
            AdventureDependency(AdventureComponent.Text.Serializer.LEGACY, textSerializerLegacyVersion, configuration) {
                exclude("org.jetbrains", "annotations")
            },
            AdventureDependency(AdventureComponent.Platform.BUKKIT, platformBukkitVersion, configuration))
    }
}
