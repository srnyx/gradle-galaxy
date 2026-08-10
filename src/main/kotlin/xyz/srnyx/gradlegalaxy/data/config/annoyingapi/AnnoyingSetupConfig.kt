package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.config.DependencyExtension
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupExtension
import xyz.srnyx.gradlegalaxy.utility.hasJavaPlugin
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import javax.inject.Inject


data class AnnoyingSetupConfig(
    val addPlatformsResourceFile: Boolean = true,
)

abstract class AnnoyingSetupExtension @Inject constructor(
    objects: ObjectFactory,
) {
    var configured: Boolean = false

    val metadata = objects.newInstance(MetadataExtension::class.java)
    val customRuntimeLibraries = objects.newInstance(CustomRuntimeLibrariesExtension::class.java)

    fun metadata(action: MetadataExtension.() -> Unit) = metadata.action()
    fun customRuntimeLibraries(action: CustomRuntimeLibrariesExtension.() -> Unit) = customRuntimeLibraries.action()

    internal fun setup(
        project: Project,
        annoyingApiDependency: DependencyExtension,
        java: JavaSetupExtension,
    ) {
        check(project.hasJavaPlugin()) { "Java plugin is not applied!" }
        check(project.hasShadowPlugin()) { "Shadow plugin is required for Annoying API!" }

        val annoyingMetadata: AnnoyingMetadata? = metadata.process(project, annoyingApiDependency, java)

        customRuntimeLibraries.process(project, annoyingMetadata)
    }
}
