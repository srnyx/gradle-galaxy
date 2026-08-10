package xyz.srnyx.gradlegalaxy.data.config.publishing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.apply
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.getPublishing
import javax.inject.Inject


/**
 * Configuration for publishing using environment variables
 *
 * @param mavenUrlEnv The environment variable to use for the Maven URL (default: `MAVEN_URL`)
 * @param usernameEnv The environment variable to use for the username (default: `MAVEN_NAME`)
 * @param passwordEnv The environment variable to use for the password (default: `MAVEN_SECRET`)
 * @param mavenUrl The URL of the Maven repository to publish to. Attempts to use [mavenUrlEnv] if null
 */
data class PublishingEnvConfig(
    var mavenUrlEnv: String = "MAVEN_URL",
    var usernameEnv: String = "MAVEN_NAME",
    var passwordEnv: String = "MAVEN_SECRET",
    var mavenUrl: String? = null,
) {
    internal fun toExtension(): PublishingEnvExtension.() -> Unit = {
        val config: PublishingEnvConfig = this@PublishingEnvConfig

        mavenUrlEnv.set(config.mavenUrlEnv)
        usernameEnv.set(config.usernameEnv)
        passwordEnv.set(config.passwordEnv)
        mavenUrl.set(config.mavenUrl)
    }
}

abstract class PublishingEnvExtension @Inject constructor(
    objects: ObjectFactory
) {
    var configured: Boolean = false

    @get:Input @get:Optional
    val mavenUrlEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_URL")
    @get:Input @get:Optional
    val usernameEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_NAME")
    @get:Input @get:Optional
    val passwordEnv: Property<String> = objects.property(String::class.java).convention("MAVEN_SECRET")
    @get:Input @get:Optional
    val mavenUrl: Property<String> = objects.property(String::class.java)

    fun setup(project: Project) {
        val extension: PublishingEnvExtension = this@PublishingEnvExtension

        project.apply(plugin = "maven-publish")

        // Create repository
        val resolvedMavenUrl = extension.mavenUrl.orNull ?: getEnvironmentVariable(extension.mavenUrlEnv.get())
        if (resolvedMavenUrl != null) project.getPublishing().repositories.maven {
            url = project.uri(resolvedMavenUrl)

            val usernameEnv = getEnvironmentVariable(extension.usernameEnv.get())
            val passwordEnv = getEnvironmentVariable(extension.passwordEnv.get())
            if (usernameEnv != null || passwordEnv != null) credentials {
                if (usernameEnv != null) username = usernameEnv
                if (passwordEnv != null) password = passwordEnv
            }
        }
    }
}
