package xyz.srnyx.gradlegalaxy.data


/**
 * Configuration for [xyz.srnyx.gradlegalaxy.utility.setupJda]
 *
 * @param mainClassName The main class name of the project (example: `xyz.srnyx.lazylibrary.LazyLibrary`)
 * @param excludeOpus Whether to exclude the `opus-java` dependency from JDA (
 */
data class JdaSetupConfig(
    val mainClassName: String? = null,
    val excludeOpus: Boolean = true,
)