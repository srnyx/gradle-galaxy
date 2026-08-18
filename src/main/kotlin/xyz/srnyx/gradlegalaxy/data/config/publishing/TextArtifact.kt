package xyz.srnyx.gradlegalaxy.data.config.publishing

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import xyz.srnyx.gradlegalaxy.extensions.TextArtifactExtension


data class TextArtifact(
    val text: () -> String,
    val classifier: String,
    val extension: String? = null,
) {
    internal fun toExtension(
        objects: ObjectFactory,
        providers: ProviderFactory
    ): TextArtifactExtension = objects.newInstance(TextArtifactExtension::class.java).apply {
        val data = this@TextArtifact

        this.text.set(providers.provider { data.text() })
        this.classifier.set(data.classifier)
        this.extension.set(data.extension)
    }
}
