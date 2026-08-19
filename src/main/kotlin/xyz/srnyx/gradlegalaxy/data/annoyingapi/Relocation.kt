package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.extensions.RelocateExtension


@Serializable
data class Relocation(
    val from: String,
    val to: String? = null,
) {
    internal fun toExtension(objects: ObjectFactory): RelocateExtension = objects.newInstance(RelocateExtension::class.java).apply {
        val data = this@Relocation

        this.from.set(data.from)
        data.to?.let { this.to.set(it) }
    }
}

