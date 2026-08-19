package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable
import xyz.srnyx.gradlegalaxy.extensions.RelocateExtension


@Serializable
data class Relocation(
    val from: String,
    val to: String? = null,
) {
    internal fun toExtension(): RelocateExtension.() -> Unit = {
        val data = this@Relocation

        this.from.set(data.from)
        data.to?.let { this.to.set(it) }
    }
}

