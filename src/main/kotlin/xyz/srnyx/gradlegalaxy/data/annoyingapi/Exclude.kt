package xyz.srnyx.gradlegalaxy.data.annoyingapi

import kotlinx.serialization.Serializable


@Serializable
data class Exclude(
    val group: String,
    val module: String,
)
