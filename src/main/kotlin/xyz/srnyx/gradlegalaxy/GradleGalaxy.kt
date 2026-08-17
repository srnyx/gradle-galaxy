package xyz.srnyx.gradlegalaxy

import org.gradle.api.Plugin
import org.gradle.api.Project
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.extensions.GradleGalaxyExtension


@Used
class GradleGalaxy : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("galaxy", GradleGalaxyExtension::class.java)
    }
}
