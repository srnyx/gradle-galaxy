package xyz.srnyx.gradlegalaxy.extensions.dependencies

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import xyz.srnyx.gradlegalaxy.data.config.DependencyExtension
import xyz.srnyx.gradlegalaxy.enums.Repository
import javax.inject.Inject


abstract class JUnitBomExtension @Inject constructor() : DependencyExtension(
    repositories = listOf(Repository.MAVEN_CENTRAL.url),
    group = "org.junit",
    name = "junit-bom",
    configurations = listOf("testImplementation"),
) {
    override fun add(project: Project) {
        // Add other required dependencies
        project.dependencies {
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        super.add(project)
    }
}
