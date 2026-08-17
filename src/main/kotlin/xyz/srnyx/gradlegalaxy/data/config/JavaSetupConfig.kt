package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import xyz.srnyx.gradlegalaxy.extensions.JavaExtension


/**
 * Configuration for `galaxy { java { } }`
 *
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (defaults to current [Project.getVersion] -> (if in GitHub workflow: `GITHUB_REF_NAME` -> `GITHUB_SHA`) -> `dev`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param archiveClassifier The archive classifier for the shadow jar task
 * @param textEncoding The text encoding for the text encoding task
 */
data class JavaSetupConfig(
    val group: String? = null,
    val version: String? = null,
    val description: String? = null,
    val javaVersion: JavaVersion? = null,
    val archiveClassifier: String? = "",
    val textEncoding: String? = "UTF-8",
) {
    internal fun toExtension(project: Project): JavaExtension.() -> Unit = {
        val config = this@JavaSetupConfig

        config.group?.let { project.group = it }
        version.set(config.version)
        config.description?.let { project.description = it }
        javaVersion.set(config.javaVersion)
        archiveClassifier.set(config.archiveClassifier)
        textEncoding.set(config.textEncoding)
    }
}
