package xyz.srnyx.gradlegalaxy.data.config.publishing

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.data.pom.DeveloperData
import xyz.srnyx.gradlegalaxy.data.pom.LicenseData
import xyz.srnyx.gradlegalaxy.data.pom.ScmData
import xyz.srnyx.gradlegalaxy.utility.addJavadocSourcesJars
import xyz.srnyx.gradlegalaxy.utility.getPublishing
import xyz.srnyx.gradlegalaxy.utility.silenceMissingJavaDocWarnings
import javax.inject.Inject


/**
 * Configuration for a dependency, including version, configuration, and configuration action
 *
 * @param project The Gradle project
 * @param groupId The group ID
 * @param artifactId The artifact ID
 * @param version The version
 * @param withJavadocSourcesJars Whether to add Javadoc and Sources JARs to the publication
 * @param silenceMissingJavadocWarnings Whether to silence missing Javadoc warnings
 * @param component The [SoftwareComponent] to publish
 * @param artifacts The artifacts to publish
 * @param textArtifacts The text artifacts to publish
 * @param name The name of the project
 * @param description The description of the project
 * @param url The URL of the project
 * @param licenses The licenses of the project
 * @param developers The developers of the project
 * @param scm The SCM information of the project
 */
data class PublishingSimpleConfig(
    var project: Project,
    var groupId: String? = null,
    var artifactId: String? = null,
    var version: String? = null,
    var withJavadocSourcesJars: Boolean = true,
    var silenceMissingJavadocWarnings: Boolean = false,
    var component: SoftwareComponent? = project.components["java"],
    var artifacts: Collection<Any> = emptyList(),
    var textArtifacts: Collection<TextArtifact> = emptyList(),
    var name: String? = project.name,
    var description: String? = project.description,
    var url: String? = null,
    var licenses: List<LicenseData> = emptyList(),
    var developers: List<DeveloperData> = emptyList(),
    val scm: ScmData? = null,
) {
    internal fun toExtension(): PublishingSimpleExtension.() -> Unit = {
        val config: PublishingSimpleConfig = this@PublishingSimpleConfig
        
        groupId.set(config.groupId)
        artifactId.set(config.artifactId)
        version.set(config.version)
        withJavadocSourcesJars.set(config.withJavadocSourcesJars)
        silenceMissingJavadocWarnings.set(config.silenceMissingJavadocWarnings)
        component.set(config.component)
        artifacts.set(config.artifacts)
        textArtifacts.set(config.textArtifacts)
        licenses.set(config.licenses)
        developers.set(config.developers)
        scm.set(config.scm)

        publication {
            pom {
                name.set(config.name)
                description.set(config.description)
                url.set(config.url)
            }
        }
    }
}

/**
 * See [PublishingSimpleConfig]
 */
@Used
fun Project.publishingSimpleConfig(
    groupId: String? = null,
    artifactId: String? = null,
    version: String? = null,
    withJavadocSourcesJars: Boolean = true,
    silenceMissingJavadocWarnings: Boolean = false,
    component: SoftwareComponent? = project.components["java"],
    artifacts: Collection<Any> = emptyList(),
    textArtifacts: Collection<TextArtifact> = emptyList(),
    name: String? = project.name,
    description: String? = project.description,
    url: String? = null,
    licenses: List<LicenseData> = emptyList(),
    developers: List<DeveloperData> = emptyList(),
    scm: ScmData? = null,
) = PublishingSimpleConfig(project, groupId, artifactId, version, withJavadocSourcesJars, silenceMissingJavadocWarnings, component, artifacts, textArtifacts, name, description, url, licenses, developers, scm)

abstract class PublishingSimpleExtension @Inject constructor(
    objects: ObjectFactory
) {
    var configured: Boolean = false

    @get:Input @get:Optional
    val groupId: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val artifactId: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val version: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val withJavadocSourcesJars: Property<Boolean> = objects.property<Boolean>(Boolean::class.java).convention(true)
    @get:Input @get:Optional
    val silenceMissingJavadocWarnings: Property<Boolean> = objects.property<Boolean>(Boolean::class.java).convention(false)
    @get:Input @get:Optional
    val component: Property<SoftwareComponent> = objects.property(SoftwareComponent::class.java)
    @get:Input @get:Optional
    val artifacts: ListProperty<Any> = objects.listProperty(Any::class.java)
    @get:Input @get:Optional
    val textArtifacts: ListProperty<TextArtifact> = objects.listProperty(TextArtifact::class.java)
    @get:Input @get:Optional
    val licenses: ListProperty<LicenseData> = objects.listProperty(LicenseData::class.java)
    @get:Input @get:Optional
    val developers: ListProperty<DeveloperData> = objects.listProperty(DeveloperData::class.java)
    @get:Input @get:Optional
    val scm: Property<ScmData> = objects.property(ScmData::class.java)

    var publicationAction: MavenPublication.() -> Unit = {}

    fun publication(action: MavenPublication.() -> Unit) {
        val previous = publicationAction
        publicationAction = {
            previous(this)
            action(this)
        }
    }

    fun setup(project: Project) {
        val extension: PublishingSimpleExtension = this@PublishingSimpleExtension

        project.apply(plugin = "maven-publish")

        // Javadocs and sources
        if (withJavadocSourcesJars.get()) project.addJavadocSourcesJars()

        // Silence missing Javadoc warnings
        if (silenceMissingJavadocWarnings.get()) project.silenceMissingJavaDocWarnings()

        // Create publication
        project.getPublishing().publications.create<MavenPublication>("maven") {
            extension.groupId.orNull?.let { this.groupId = it }
            extension.artifactId.orNull?.let { this.artifactId = it }
            extension.version.orNull?.let { this.version = it }
            
            from(extension.component.getOrElse(project.components["java"]))
            
            extension.artifacts.orNull?.forEach(this::artifact)
            textArtifacts.orNull?.forEach { textArtifact ->
                val taskName = "generate${textArtifact.classifier.capitalized()}TextArtifact"
                val extensionSuffix = textArtifact.extension?.let { ".$it" } ?: ""
                val outputFile = project.layout.buildDirectory.file("generated/publications/${this.artifactId}-${this.version}-${textArtifact.classifier}$extensionSuffix")

                val textProvider = project.provider { textArtifact.text.invoke() }
                val task = project.tasks.register(taskName) {
                    group = "publishing"
                    description = "Generates the ${textArtifact.classifier} artifact for publication ${this.name}"

                    inputs.property("text", textProvider)
                    outputs.file(outputFile)

                    doLast {
                        outputFile.get().asFile.apply {
                            parentFile.mkdirs()
                            writeText(textProvider.get())
                        }
                    }
                }

                artifact(outputFile) {
                    this.classifier = textArtifact.classifier
                    this.extension = textArtifact.extension
                    builtBy(task)
                }
            }
            pom {
                name.set(project.name)
                description.set(project.description)
                
                licenses { extension.licenses.orNull?.forEach { license {
                    this.name.set(it.name)
                    this.url.set(it.url)
                    it.distribution?.value?.let(this.distribution::set)
                    it.comments?.let(this.comments::set)
                } } }
                
                developers { extension.developers.orNull
                    ?.filterNot(DeveloperData::isEmpty)
                    ?.forEach { developer {
                        it.id?.let(this.id::set)
                        it.name?.let(this.name::set)
                        it.url?.let(this.url::set)
                        it.email?.let(this.email::set)
                        it.timezone?.let(this.timezone::set)
                        it.organization?.let(this.organization::set)
                        it.organizationUrl?.let(this.organizationUrl::set)
                        it.roles.takeIf(List<String>::isNotEmpty)?.let(this.roles::set)
                        it.properties.takeIf(Map<String, String>::isNotEmpty)?.let(this.properties::set)
                    } } }
                
                extension.scm.orNull?.let { scm -> scm {
                    connection.set(scm.connection)
                    developerConnection.set(scm.developerConnection)
                    scm.url?.let(this.url::set)
                    scm.tag?.let(this.tag::set)
                } }
            }

            publicationAction(this)
        }
    }
}
