package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.Project
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.setupMC
import javax.inject.Inject


/**
 * Configuration for [setupMC]
 *
 * @param replacementFiles The files to apply replacements to (default: `plugin.yml`)
 * @param replacements The replacements for the [replacements task][addReplacementsTask] (default: `defaultReplacements` to `true`)
 */
data class MCSetupConfig(
    val replacementFiles: Set<String>? = setOf("plugin.yml"),
    val replacements: Map<String, String>? = mapOf("defaultReplacements" to "true"),
) {
    internal fun toExtension(): MCSetupExtension.() -> Unit = {
        if (this@MCSetupConfig.replacementFiles == null || this@MCSetupConfig.replacements == null) {
            replacementFiles = emptySet()
            replacements = emptyMap()
        } else {
            replacementFiles = this@MCSetupConfig.replacementFiles
            replacements = this@MCSetupConfig.replacements
        }
    }
}

abstract class MCSetupExtension @Inject constructor() {
    var configured: Boolean = false

    var replacementFiles: Set<String> = setOf("plugin.yml")
    var replacements: Map<String, String> = mapOf("defaultReplacements" to "true")

    internal fun setup(project: Project) {
        project.addReplacementsTask(replacementFiles, replacements)
    }
}
