package xyz.srnyx.gradlegalaxy.data.config

import xyz.srnyx.gradlegalaxy.extensions.MinecraftExtension


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
        if (this@MCSetupConfig.replacementFiles == null || this@MCSetupConfig.replacements == null) {
            replacementFiles = emptySet()
            replacements = emptyMap()
        } else {
            replacementFiles = this@MCSetupConfig.replacementFiles
            replacements = this@MCSetupConfig.replacements
        }
    }
}
