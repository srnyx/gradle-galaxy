package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.srnyx.gradlegalaxy.enums.PaperVersion
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.utility.getJavaVersionForMC
import xyz.srnyx.gradlegalaxy.utility.getVersionString
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import javax.inject.Inject


abstract class PaperExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(
    repositories = listOf(Repository.MAVEN_CENTRAL.url, Repository.SONATYPE_SNAPSHOTS_OLD.url, Repository.PAPER.url),
    group = "io.papermc.paper",
    name = "paper-api",
    configurations = listOf("compileOnly", "testImplementation"),
) {
    @get:Input @get:Optional
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Runs in [xyz.srnyx.gradlegalaxy.extensions.Phase.WIRE] — see [xyz.srnyx.gradlegalaxy.extensions.SpigotApiExtension.setup]'s KDoc for why.
     */
    internal fun setup(project: Project) {
        if (setJavaVersion.get()) project.setJavaVersion(getJavaVersionForMC(version))
    }

    override fun add(project: Project) {
        val paperVersion = PaperVersion.parse(version)
        group = paperVersion.groupId
        name = paperVersion.artifactId
        version = getVersionString(version)

        super.add(project)
    }
}
