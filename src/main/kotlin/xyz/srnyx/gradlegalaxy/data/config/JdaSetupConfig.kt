package xyz.srnyx.gradlegalaxy.data.config

import xyz.srnyx.gradlegalaxy.extensions.JdaExtension


/**
 * Configuration for `galaxy { jda { } }`
 *
 * @param mainClassName The main class name of the project (example: `xyz.srnyx.lazylibrary.LazyLibrary`)
 * @param excludeOpus Whether to exclude the `opus-java` dependency from JDA
 */
data class JdaSetupConfig(
    var mainClassName: String? = null,
    var excludeOpus: Boolean = true,
) {
    internal fun toExtension(): JdaExtension.() -> Unit = {
        mainClassName.set(this@JdaSetupConfig.mainClassName)
        excludeOpus.set(this@JdaSetupConfig.excludeOpus)
    }
}
