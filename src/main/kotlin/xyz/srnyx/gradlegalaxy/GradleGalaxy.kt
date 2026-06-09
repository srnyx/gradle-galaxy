package xyz.srnyx.gradlegalaxy

import org.gradle.api.Plugin
import org.gradle.api.Project
import xyz.srnyx.gradlegalaxy.annotations.Used


@Used
class GradleGalaxy : Plugin<Project> {
    override fun apply(target: Project) = Unit
}
