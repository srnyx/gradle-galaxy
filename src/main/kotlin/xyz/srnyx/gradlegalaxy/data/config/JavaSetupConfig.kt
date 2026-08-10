package xyz.srnyx.gradlegalaxy.data.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.utility.addBuildShadowTask
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import xyz.srnyx.gradlegalaxy.utility.setShadowArchiveClassifier
import xyz.srnyx.gradlegalaxy.utility.setTextEncoding
import xyz.srnyx.gradlegalaxy.utility.setupJava
import javax.inject.Inject


/**
 * Configuration for [setupJava]
 *
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (defaults to current [Project.getVersion] -> (if in GitHub workflow: `GITHUB_REF_NAME` -> `GITHUB_SHA`) -> `dev`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param archiveClassifier The archive classifier for the [shadow jar task][setShadowArchiveClassifier]
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 */
data class JavaSetupConfig(
    val group: String? = null,
    val version: String? = null,
    val description: String? = null,
    val javaVersion: JavaVersion? = null,
    val archiveClassifier: String? = "",
    val textEncoding: String? = "UTF-8",
) {
    internal fun toExtension(): JavaSetupExtension.() -> Unit = {
        group = this@JavaSetupConfig.group
        version = this@JavaSetupConfig.version
        description = this@JavaSetupConfig.description
        javaVersion = this@JavaSetupConfig.javaVersion
        archiveClassifier = this@JavaSetupConfig.archiveClassifier
        textEncoding = this@JavaSetupConfig.textEncoding
    }
}

abstract class JavaSetupExtension @Inject constructor() {
    var configured: Boolean = false

    var group: String? = null
    var version: String? = null
    var description: String? = null
    var javaVersion: JavaVersion? = null
    var archiveClassifier: String? = ""
    var textEncoding: String? = "UTF-8"

    internal fun setup(project: Project) {
        project.group = group ?: project.group
        project.version = version
            ?: project.version.takeIf { it != Project.DEFAULT_VERSION }
            ?: when {
                inGitHubWorkflow -> getEnvironmentVariable("GITHUB_REF_NAME")
                    ?.takeIf { inGitHubPublish }
                    ?: getEnvironmentVariable("GITHUB_SHA")?.take(7)
                else -> null
            }
            ?: "dev"
        project.description = description ?: project.description

        javaVersion?.let { project.setJavaVersion(it) }
        textEncoding?.let { project.setTextEncoding(it) }

        if (project.hasShadowPlugin()) {
            archiveClassifier?.let { project.setShadowArchiveClassifier(it) }
            project.addBuildShadowTask()
        }
    }
}
