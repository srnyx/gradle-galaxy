package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.srnyx.gradlegalaxy.utility.addBuildShadowTask
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import xyz.srnyx.gradlegalaxy.utility.setShadowArchiveClassifier
import xyz.srnyx.gradlegalaxy.utility.setTextEncoding
import javax.inject.Inject


/**
 * Fires immediately when `java { }` is called, unlike every other `galaxy { }` entry.
 * It only ever *writes* plain [Project] state (`group`/`version`/`description`/etc.), and nothing
 * reads that state until every other entry's deferred [Phase.WIRE] actions run ([DeferredActions]),
 * so there's no ordering hazard in applying it eagerly.
 */
abstract class JavaExtension @Inject constructor(
    objects: ObjectFactory,
) {
    @get:Input @get:Optional
    val group: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val version: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val description: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val javaVersion: Property<JavaVersion> = objects.property(JavaVersion::class.java)
    @get:Input
    val archiveClassifier: Property<String> = objects.property(String::class.java).convention("")
    @get:Input
    val textEncoding: Property<String> = objects.property(String::class.java).convention("UTF-8")

    private var applied = false

    /**
     * Idempotent: multiple `galaxy { }` entries (top-level `java { }`, and any entry that bundles java
     * setup in automatically, like `annoyingAPI { }`) may all call this. Only the first actually applies.
     */
    internal fun setup(project: Project) {
        if (applied) return
        applied = true

        project.group = group.getOrElse(project.group.toString())
        project.version = version.orNull
            ?: project.version.takeIf { it != Project.DEFAULT_VERSION }
            ?: when {
                project.inGitHubWorkflow -> project.getEnvironmentVariable("GITHUB_REF_NAME")
                    ?.takeIf { project.inGitHubPublish }
                    ?: project.getEnvironmentVariable("GITHUB_SHA")?.take(7)
                else -> null
            }
            ?: "snapshot"
        project.description = description.getOrElse(project.description.toString())

        javaVersion.orNull?.let { project.setJavaVersion(it, force = true) }
        textEncoding.orNull?.let { project.setTextEncoding(it) }

        if (project.hasShadowPlugin()) {
            archiveClassifier.orNull?.let { project.setShadowArchiveClassifier(it) }
            project.addBuildShadowTask()
        }
    }
}
