package xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.networking.RequestContext.Default.json
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.withType
import xyz.srnyx.gradlegalaxy.enums.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.ReleaseChannel
import xyz.srnyx.gradlegalaxy.utility.getEnvironmentVariable
import xyz.srnyx.gradlegalaxy.utility.hasShadowPlugin
import java.io.File


internal fun PlatformPublishingExtension.setupModPublish(
    project: Project,
    releaseChannel: ReleaseChannel,
    changelogText: String,
    primaryFile: Provider<RegularFile>,
) {
    val modrinthIdentifier = platforms.get()[PluginPlatform.MODRINTH]
    val curseForgeIdentifier = platforms.get()[PluginPlatform.CURSEFORGE]

    project.extensions.configure<ModPublishExtension>("publishMods") {
        dryRun.set(this@setupModPublish.dryRun)
        modLoaders.set(computeLoaders())
        type.set(releaseChannel.mpp)
        changelog.set(changelogText)

        // Display name
        val event = project.getEnvironmentVariable("GITHUB_EVENT_PATH")
            ?.let { json.decodeFromString<JsonObject>(File(it).readText()) }
        displayName.set(event
            // Release name
            ?.get("release")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            // Commit name
            ?: event?.get("commits")?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?.lines()?.firstOrNull() // Only use commit title/summary, remove description
            // Project version
            ?: project.version.toString())

        // Primary file (shadowJar or jar)
        file.set(primaryFile)

        // Additional files (javadocJar and sourcesJar)
        val javadocJarTask = project.tasks.findByName("javadocJar") as? Jar
        val sourcesJarTask = project.tasks.findByName("sourcesJar") as? Jar
        javadocJarTask?.let { additionalFiles.from(it) }
        sourcesJarTask?.let { additionalFiles.from(it) }

        val minecraftVersionEnd = minecraftVersionEnd.getOrElse("latest")

        // Modrinth
        if (modrinthIdentifier != null) {
            val token = project.getEnvironmentVariable("MODRINTH_TOKEN")
            if (dryRun.get() || token != null) modrinth {
                accessToken.set(token)
                minecraftVersionRange {
                    start.set(minecraftVersionStart.get())
                    end.set(minecraftVersionEnd)
                }

                // Annoying API dependency
                if (addAnnoyingApiDependency.get()) embeds("annoying-api")

                // Universal dependencies
                minecraft.dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addModrinth(this) }

                // Additional file types
                javadocJarTask?.let { additionalFile(it.archiveFile) { type.set(JAVADOC_JAR) } }
                sourcesJarTask?.let { additionalFile(it.archiveFile) { type.set(SOURCES_JAR) } }

                projectId.set(modrinthIdentifier)
                this@setupModPublish.modrinth?.invoke(this)
            }
        }

        // CurseForge
        if (curseForgeIdentifier != null) {
            val token = project.getEnvironmentVariable("CURSEFORGE_TOKEN")
            if (dryRun.get() || token != null) curseforge {
                accessToken.set(project.getEnvironmentVariable("CURSEFORGE_TOKEN"))
                minecraftVersionRange {
                    start.set(minecraftVersionStart.get())
                    end.set(minecraftVersionEnd)
                }

                // Annoying API dependency
                if (addAnnoyingApiDependency.get()) embeds("annoying-api")

                // Universal dependencies
                minecraft.dependency.universalDependencies.orNull?.forEach { dependency -> dependency.addCurseforge(this) }

                projectId.set(curseForgeIdentifier)
                this@setupModPublish.curseforge?.invoke(this)
            }
        }

        // Ensure publishing runs after building
        project.tasks.withType<PublishModTask> {
            dependsOn("jar")
            if (project.hasShadowPlugin()) dependsOn("shadowJar")
        }

        this@setupModPublish.modPublishPlugin?.invoke(this)
    }
}
