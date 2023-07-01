package xyz.srnyx.gradlegalaxy.data

import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency

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
data class AdventureDependency(val component: Component, val version: String, val configuration: String? = null, val configurationAction: Action<ExternalModuleDependency> = Action {}) {
    companion object {
        /**
         * Gets the default dependencies for Adventure for Annoying API
         *
         * @param configuration The configuration to add the dependencies to
         * @param apiVersion The version of [AdventureComponent.API] to depend on
         * @param textMinimessageVersion The version of [AdventureComponent.Text.MINIMESSAGE] to depend on
         * @param textSerializerLegacyVersion The version of [AdventureComponent.Text.Serializer.LEGACY] to depend on
         * @param platformBungeecordVersion The version of [AdventureComponent.Platform.BUNGEECORD] to depend on
         *
         * @return The default dependencies for Adventure for Annoying API
         */
        fun getDefaultAnnoying(
            configuration: String = "implementation",
            apiVersion: String = "4.14.0",
            textMinimessageVersion: String = "4.14.0",
            textSerializerLegacyVersion: String = "4.14.0",
            textSerializerPlainVersion: String = "4.14.0",
            platformBukkitVersion: String = "4.3.0",
            platformBungeecordVersion: String = "4.3.0",
        ): Array<AdventureDependency> = arrayOf(
            AdventureDependency(AdventureComponent.API, apiVersion, configuration),
            AdventureDependency(AdventureComponent.Text.MINIMESSAGE, textMinimessageVersion, configuration),
            AdventureDependency(AdventureComponent.Text.Serializer.LEGACY, textSerializerLegacyVersion, configuration),
            AdventureDependency(AdventureComponent.Text.Serializer.PLAIN, textSerializerPlainVersion, configuration),
            AdventureDependency(AdventureComponent.Platform.BUKKIT, platformBukkitVersion, configuration),
            AdventureDependency(AdventureComponent.Platform.BUNGEECORD, platformBungeecordVersion, configuration))
    }
}
