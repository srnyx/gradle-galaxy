package xyz.srnyx.gradlegalaxy.data.config.publishing


/**
 * Configuration for publishing using environment variables
 *
 * @param mavenUrlEnv The environment variable to use for the Maven URL (default: `MAVEN_URL`)
 * @param versionEnv The environment variable to use for the version (default: `VERSION`)
 * @param usernameEnv The environment variable to use for the username (default: `MAVEN_NAME`)
 * @param passwordEnv The environment variable to use for the password (default: `MAVEN_SECRET`)
 * @param mavenUrl The URL of the Maven repository to publish to. Attempts to use [mavenUrlEnv] if null.
 * @param defaultVersion The default version to use if the environment variable is not set (default: `dev`). Set to `null` to use project's version.
 */
data class PublishingEnvConfig(
    var mavenUrlEnv: String = "MAVEN_URL",
    var versionEnv: String = "VERSION",
    var usernameEnv: String = "MAVEN_NAME",
    var passwordEnv: String = "MAVEN_SECRET",
    var mavenUrl: String? = null,
    var defaultVersion: String? = "dev",
)
