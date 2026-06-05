package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable


@Serializable
data class Relocation(
    val from: String,
    val to: String? = null,
)
