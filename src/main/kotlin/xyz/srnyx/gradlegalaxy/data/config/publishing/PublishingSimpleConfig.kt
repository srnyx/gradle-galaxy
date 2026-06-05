package xyz.srnyx.gradlegalaxy.data.config.publishing

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.data.pom.LicenseData
import xyz.srnyx.gradlegalaxy.data.pom.ScmData


/**
 * Configuration for a dependency, including version, configuration, and configuration action
 *
 * @param project The Gradle project
 * @param groupId The group ID
 * @param artifactId The artifact ID
 * @param version The version
 * @param withJavadocSourcesJars Whether to add Javadoc and Sources JARs to the publication
 * @param component The [SoftwareComponent] to publish
 * @param artifacts The artifacts to publish
 * @param textArtifacts The text artifacts to publish
 * @param name The name of the project
 * @param description The description of the project
 * @param url The URL of the project
 * @param licenses The licenses of the project
 * @param developers The developers of the project
 * @param scm The SCM information of the project
 * @param configuration The configuration of the [MavenPublication]
 */
data class PublishingSimpleConfig(
    var project: Project,
    var groupId: String? = null,
    var artifactId: String? = null,
    var version: String? = null,
    var withJavadocSourcesJars: Boolean = true,
    var component: SoftwareComponent? = project.components["java"],
    var artifacts: Collection<Any> = emptyList(),
    var textArtifacts: Collection<TextArtifact> = emptyList(),
    var name: String? = project.name,
    var description: String? = project.description,
    var url: String? = null,
    var licenses: List<LicenseData> = emptyList(),
    var developers: List<DeveloperData> = emptyList(),
    val scm: ScmData? = null,
    var configuration: MavenPublication.() -> Unit = {}
)

/**
 * See [PublishingSimpleConfig]
 */
fun Project.publishingSimpleConfig(
    groupId: String? = null,
    artifactId: String? = null,
    version: String? = null,
    withJavadocSourcesJars: Boolean = true,
    component: SoftwareComponent? = project.components["java"],
    artifacts: Collection<Any> = emptyList(),
    textArtifacts: Collection<TextArtifact> = emptyList(),
    name: String? = project.name,
    description: String? = project.description,
    url: String? = null,
    licenses: List<LicenseData> = emptyList(),
    developers: List<DeveloperData> = emptyList(),
    scm: ScmData? = null,
    configuration: MavenPublication.() -> Unit = {}
) = PublishingSimpleConfig(project, groupId, artifactId, version, withJavadocSourcesJars, component, artifacts, textArtifacts, name, description, url, licenses, developers, scm, configuration)
