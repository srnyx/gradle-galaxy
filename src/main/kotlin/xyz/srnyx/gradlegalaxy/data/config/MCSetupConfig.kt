package xyz.srnyx.gradlegalaxy.data.config

import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.setupMC


/**
 * Configuration for [setupMC]
 *
 * @param replacementFiles The files to apply replacements to (default: `plugin.yml`)
 * @param replacements The replacements for the [replacements task][addReplacementsTask] (default: `defaultReplacements` to `true`)
 */
data class MCSetupConfig(
    val replacementFiles: Set<String>? = setOf("plugin.yml"),
    val replacements: Map<String, String>? = mapOf("defaultReplacements" to "true"),
)