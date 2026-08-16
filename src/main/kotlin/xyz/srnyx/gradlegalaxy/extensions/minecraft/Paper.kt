package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.srnyx.gradlegalaxy.enums.PaperVersion
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import xyz.srnyx.gradlegalaxy.utility.setJavaVersion
import javax.inject.Inject


abstract class PaperExtension @Inject constructor(
    objects: ObjectFactory
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL, REPOSITORIES.SONATYPE_SNAPSHOTS_OLD, REPOSITORIES.PAPER))
        group.set("io.papermc.paper")
        name.set("paper-api")
        configurations.set(listOf("compileOnly", "testImplementation"))
    } }
    @get:Input @get:Optional
    val setJavaVersion: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Runs in [xyz.srnyx.gradlegalaxy.extensions.Phase.WIRE] — see [SpigotApiExtension.setup]'s KDoc for why.
     */
    internal fun setup(project: Project) {
        if (setJavaVersion.get()) project.setJavaVersion(getJavaVersionForMC(version.get()))
    }

    override fun add(project: Project) {
        val paperVersion = PaperVersion.parse(version.get())
        group.set(paperVersion.groupId)
        name.set(paperVersion.artifactId)
        version.set(getVersionString(version.get()))

        super.add(project)
    }
}
