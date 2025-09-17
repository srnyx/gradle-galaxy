package xyz.srnyx.gradlegalaxy.data

import org.gradle.api.JavaVersion

import xyz.srnyx.gradlegalaxy.utility.setShadowArchiveClassifier
import xyz.srnyx.gradlegalaxy.utility.setTextEncoding
import xyz.srnyx.gradlegalaxy.utility.setupJava


/**
 * Configuration for [setupJava]
 *
 * @param group The group of the project (example: `xyz.srnyx`)
 * @param version The version of the project (example: `1.0.0`)
 * @param description The description of the project
 * @param javaVersion The java version of the project (example: [JavaVersion.VERSION_1_8])
 * @param archiveClassifier The archive classifier for the [shadow jar task][setShadowArchiveClassifier]
 * @param textEncoding The text encoding for the [text encoding task][setTextEncoding]
 */
data class JavaSetupConfig(
    val group: String? = null,
    val version: String? = null,
    val description: String? = null,
    val javaVersion: JavaVersion? = null,
    val archiveClassifier: String? = "",
    val textEncoding: String? = "UTF-8",
)