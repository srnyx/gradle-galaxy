package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
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
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.getPublishing
import xyz.srnyx.gradlegalaxy.utility.silenceMissingJavaDocWarnings
import javax.inject.Inject


abstract class MavenPublishingExtension @Inject constructor(
    private val objects: ObjectFactory
) {
    // LicenseData
    @Used val MIT = LicenseData.MIT
    @Used val APACHE_2_0 = LicenseData.APACHE_2_0
    @Used val GPL_V3 = LicenseData.GPL_V3
    // DeveloperData
    @Used val SRNYX = DeveloperData.srnyx
    @Used val DKIM19375 = DeveloperData.dkim19375

    // Publication
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
    val textArtifacts: ListProperty<TextArtifactExtension> = objects.listProperty(TextArtifactExtension::class.java)
    @get:Input @get:Optional
    val licenses: ListProperty<LicenseData> = objects.listProperty(LicenseData::class.java)
    @get:Input @get:Optional
    val developers: ListProperty<DeveloperData> = objects.listProperty(DeveloperData::class.java)
    @get:Input @get:Optional
    val scm: Property<ScmData> = objects.property(ScmData::class.java)

    var publicationAction: MavenPublication.() -> Unit = {}

    // Repository
    @get:Input @get:Optional
    val mavenUrlEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_URL")
    @get:Input @get:Optional
    val usernameEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_NAME")
    @get:Input @get:Optional
    val passwordEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_SECRET")
    @get:Input @get:Optional
    val mavenUrl: Property<String> = objects.property(String::class.java)


    fun textArtifact(action: TextArtifactExtension.() -> Unit) {
        val textArtifact = objects.newInstance(TextArtifactExtension::class.java)
        textArtifact.action()
        textArtifacts.add(textArtifact)
    }

    fun publication(action: MavenPublication.() -> Unit) {
        val previous = publicationAction
        publicationAction = {
            previous(this)
            action(this)
        }
    }

    internal fun setup(project: Project) {
        val extension: MavenPublishingExtension = this@MavenPublishingExtension

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
                val taskName = "generate${textArtifact.classifier.get().capitalized()}TextArtifact"
                val extensionSuffix = textArtifact.extension.orNull?.let { ".$it" } ?: ""
                val outputFile = project.layout.buildDirectory.file("generated/publications/${this.artifactId}-${this.version}-${textArtifact.classifier}$extensionSuffix")

                val textProvider = project.provider { textArtifact.text.get() }
                val task = project.tasks.register(taskName) {
                    group = "galaxy"
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
                    this.classifier = textArtifact.classifier.get()
                    this.extension = textArtifact.extension.orNull
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

        // Create repository
        val resolvedMavenUrl = extension.mavenUrl.orNull ?: project.getEnvironmentVariable(extension.mavenUrlEnv.get())
        if (resolvedMavenUrl != null) project.getPublishing().repositories.maven {
            url = project.uri(resolvedMavenUrl)

            val usernameEnv = project.getEnvironmentVariable(extension.usernameEnv.get())
            val passwordEnv = project.getEnvironmentVariable(extension.passwordEnv.get())
            if (usernameEnv != null || passwordEnv != null) credentials {
                if (usernameEnv != null) username = usernameEnv
                if (passwordEnv != null) password = passwordEnv
            }
        }
    }
}

abstract class TextArtifactExtension(
    objects: ObjectFactory
) {
    @get:Input
    val text: Property<String> = objects.property(String::class.java)
    @get:Input
    val classifier: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val extension: Property<String> = objects.property(String::class.java)
}
