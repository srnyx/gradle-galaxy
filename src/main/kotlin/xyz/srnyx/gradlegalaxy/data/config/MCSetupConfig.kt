package xyz.srnyx.gradlegalaxy.data.config

import xyz.srnyx.gradlegalaxy.extensions.minecraft.MinecraftExtension


/**
 * Configuration for `galaxy { minecraft { } }`
 *
 * @param replacementFiles The files to apply replacements to (default: `plugin.yml`)
 * @param replacements The replacements for the replacements task (default: `defaultReplacements` to `true`)
 */
data class MCSetupConfig(
    val replacementFiles: Set<String>? = setOf("plugin.yml"),
    val replacements: Map<String, String>? = mapOf("defaultReplacements" to "true"),
) {
    internal fun toExtension(): MinecraftExtension.() -> Unit = {
        val config: MCSetupConfig = this@MCSetupConfig

        if (config.replacementFiles == null || config.replacements == null) {
            replacementFiles.set(emptySet())
            replacements.set(emptyMap())
        } else {
            replacementFiles.set(config.replacementFiles)
            replacements.set(config.replacements)
        }
    }
}
