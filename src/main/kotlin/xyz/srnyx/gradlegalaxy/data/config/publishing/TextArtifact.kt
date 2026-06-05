package xyz.srnyx.gradlegalaxy.data.config.publishing


data class TextArtifact(
    val text: () -> String,
    val classifier: String,
    val extension: String? = null,
)
