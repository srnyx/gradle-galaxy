package xyz.srnyx.gradlegalaxy.enums

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven
import xyz.srnyx.gradlegalaxy.extensions.GradleGalaxyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES


/**
 * Enum class for popular repositories. Use with [mavenQuick]
 */
@Deprecated("Use galaxy { repository { ... } } instead")
enum class Repository(val url: String) {
    /**
     * `mavenLocal()`
     */
    MAVEN_LOCAL("MAVEN_LOCAL"),
    /**
     * [ArtifactRepositoryContainer.MAVEN_CENTRAL_URL]
     */
    MAVEN_CENTRAL(REPOSITORIES.MAVEN_CENTRAL),
    /**
     * [https://repo.srnyx.com/releases/](https://repo.srnyx.com/releases/)
     */
    SRNYX_RELEASES(REPOSITORIES.SRNYX_RELEASES),
    /**
     * [https://repo.srnyx.com/snapshots/](https://repo.srnyx.com/snapshots/)
     */
    SRNYX_SNAPSHOTS(REPOSITORIES.SRNYX_SNAPSHOTS),
    /**
     * [https://repo.srnyx.com/private/](https://repo.srnyx.com/private/)
     */
    SRNYX_PRIVATE(REPOSITORIES.SRNYX_PRIVATE),
    /**
     * [https://hub.spigotmc.org/nexus/content/repositories/public/](https://hub.spigotmc.org/nexus/content/repositories/public/)
     */
    SPIGOT(REPOSITORIES.SPIGOT),
    /**
     * [https://hub.spigotmc.org/nexus/content/repositories/snapshots/](https://hub.spigotmc.org/nexus/content/repositories/snapshots/)
     */
    SPIGOT_SNAPSHOTS(REPOSITORIES.SPIGOT_SNAPSHOTS),
    /**
     * [https://repo.papermc.io/repository/maven-public/](https://repo.papermc.io/repository/maven-public/)
     */
    PAPER(REPOSITORIES.PAPER),
    /**
     * [https://jitpack.io/](https://jitpack.io/)
     */
    JITPACK(REPOSITORIES.JITPACK),
    /**
     * [https://repo.clojars.org/](https://repo.clojars.org/)
     */
    CLOJARS(REPOSITORIES.CLOJARS),
    /**
     * [https://m2.dv8tion.net/releases/](https://m2.dv8tion.net/releases/)
     */
    DV8TION(REPOSITORIES.DV8TION),
    /**
     * [https://repo.triumphteam.dev/releases/](https://repo.triumphteam.dev/releases/)
     */
    TRIUMPH_RELEASES(REPOSITORIES.TRIUMPH_RELEASES),
    /**
     * [https://repo.triumphteam.dev/snapshots/](https://repo.triumphteam.dev/snapshots/)
     */
    TRIUMPH_SNAPSHOTS(REPOSITORIES.TRIUMPH_SNAPSHOTS),
    /**
     * [https://repo.viaversion.com/everything/](https://repo.viaversion.com/everything/)
     */
    VIA_VERSION(REPOSITORIES.VIA_VERSION),
    /**
     * [https://repo.dmulloy2.net/repository/public/](https://repo.dmulloy2.net/repository/public/)
     */
    PROTOCOL_LIB(REPOSITORIES.PROTOCOL_LIB),
    /**
     * [https://nexus.scarsz.me/content/groups/public/](https://nexus.scarsz.me/content/groups/public/)
     */
    SCARSZ(REPOSITORIES.SCARSZ),
    /**
     * [https://repo.codemc.org/repository/maven-public/](https://repo.codemc.org/repository/maven-public/)
     */
    CODE_MC(REPOSITORIES.CODE_MC),
    /**
     * [https://oss.sonatype.org/content/repositories/releases/](https://oss.sonatype.org/content/repositories/releases/)
     */
    SONATYPE_RELEASES_OLD(REPOSITORIES.SONATYPE_RELEASES_OLD),
    /**
     * [https://oss.sonatype.org/content/repositories/snapshots/](https://oss.sonatype.org/content/repositories/snapshots/)
     */
    SONATYPE_SNAPSHOTS_OLD(REPOSITORIES.SONATYPE_SNAPSHOTS_OLD),
    /**
     * [https://s01.oss.sonatype.org/content/repositories/releases/](https://s01.oss.sonatype.org/content/repositories/releases/)
     */
    SONATYPE_RELEASES(REPOSITORIES.SONATYPE_RELEASES),
    /**
     * [https://s01.oss.sonatype.org/content/repositories/snapshots/](https://s01.oss.sonatype.org/content/repositories/snapshots/)
     */
    SONATYPE_SNAPSHOTS(REPOSITORIES.SONATYPE_SNAPSHOTS),
    /**
     * [https://nexus.umbcraft.online/repository/umbcraft-pub/](https://nexus.umbcraft.online/repository/umbcraft-pub/)
     */
    UMB_CRAFT(REPOSITORIES.UMB_CRAFT),
    /**
     * [https://repo.extendedclip.com/content/repositories/placeholderapi/](https://repo.extendedclip.com/content/repositories/placeholderapi/)
     */
    PLACEHOLDER_API(REPOSITORIES.PLACEHOLDER_API),
    /**
     * [https://repo.alessiodp.com/releases/](https://repo.alessiodp.com/releases/)
     */
    ALESSIO_DP(REPOSITORIES.ALESSIO_DP),
    /**
     * [https://repo.onarandombox.com/content/groups/public/](https://repo.onarandombox.com/content/groups/public/)
     */
    MULTIVERSE(REPOSITORIES.MULTIVERSE),
    /**
     * [https://repo.extendedclip.com/content/repositories/public/](https://repo.extendedclip.com/content/repositories/public/)
     */
    EXTENDED_CLIP(REPOSITORIES.EXTENDED_CLIP),
    /**
     * [https://maven.enginehub.org/repo/](https://maven.enginehub.org/repo/)
     */
    ENGINE_HUB(REPOSITORIES.ENGINE_HUB),
    /**
     * [https://redempt.dev/](https://redempt.dev/)
     */
    REDEMPT(REPOSITORIES.REDEMPT),
    /**
     * [https://repo.kryptonmc.org/releases/](https://repo.kryptonmc.org/releases/)
     */
    KRYPTON_RELEASES(REPOSITORIES.KRYPTON_RELEASES),
    /**
     * [https://repo.kryptonmc.org/snapshots/](https://repo.kryptonmc.org/snapshots/)
     */
    KRYPTON_SNAPSHOTS(REPOSITORIES.KRYPTON_SNAPSHOTS),
    /**
     * [https://ci.2lstudios.dev/plugin/repository/everything/](https://ci.2lstudios.dev/plugin/repository/everything/)
     */
    TWOL_STUDIOS(REPOSITORIES.TWOL_STUDIOS),
    /**
     * [https://maven.fabricmc.net/](https://maven.fabricmc.net/)
     */
    FABRIC(REPOSITORIES.FABRIC),
    /**
     * [https://maven.shedaniel.me/](https://maven.shedaniel.me/)
     */
    SHEDANIEL(REPOSITORIES.SHEDANIEL),
    /**
     * [https://maven.terraformersmc.com/releases/](https://maven.terraformersmc.com/releases/)
     */
    TERRAFORMERS(REPOSITORIES.TERRAFORMERS),
    /**
     * [https://maven.isxander.dev/releases/](https://maven.isxander.dev/releases/)
     */
    ISXANDER(REPOSITORIES.ISXANDER),
    /**
     * [https://maven.dynomake.it/releases/](https://maven.dynomake.it/releases/)
     */
    DYNOMAKE(REPOSITORIES.DYNOMAKE),
    /**
     * [https://repo.faststats.dev/releases/](https://repo.faststats.dev/releases/)
     */
    FASTSTATS_RELEASES(REPOSITORIES.FASTSTATS_RELEASES),
    /**
     * [https://repo.faststats.dev/snapshots/](https://repo.faststats.dev/snapshots/)
     */
    FASTSTATS_SNAPSHOTS(REPOSITORIES.FASTSTATS_SNAPSHOTS),
    /**
     * [https://repo.okaeri.cloud/releases/](https://repo.okaeri.cloud/releases/)
     */
    OKAERI_RELEASES(REPOSITORIES.OKAERI_RELEASES),
    /**
     * [https://repo.okaeri.cloud/snapshots/](https://repo.okaeri.cloud/snapshots/)
     */
    OKAERI_SNAPSHOTS(REPOSITORIES.OKAERI_SNAPSHOTS),
    /**
     * [https://repo.essentialsx.net/releases/](https://repo.essentialsx.net/releases/)
     */
    ESSENTIALS_RELEASES(REPOSITORIES.ESSENTIALS_RELEASES),
    /**
     * [https://repo.essentialsx.net/snapshots/](https://repo.essentialsx.net/snapshots/)
     */
    ESSENTIALS_SNAPSHOTS(REPOSITORIES.ESSENTIALS_SNAPSHOTS),
}

/**
 * Quickly add a maven repository using the [Repository] enum
 */
@Deprecated("Use galaxy { repository { ... } } instead",
    ReplaceWith("galaxy { repository(repositories.map { it.url }) }"))
fun Project.mavenQuick(vararg repositories: Repository) = extensions.configure<GradleGalaxyExtension>("galaxy") {
    repository {
        add(repositories.map { it.url })
    }
}

/**
 * Alias for [mavenQuick]
 */
@Deprecated(
    "Use galaxy { repository { ... } } instead",
    ReplaceWith("galaxy { repository(repositories.map { it.url }) }"))
fun Project.maven(vararg repositories: Repository) = mavenQuick(*repositories)

/**
 * Quickly add a maven repository using the [Repository] enum
 */
@Deprecated(
    "Use galaxy { repository { ... } } instead",
    ReplaceWith("galaxy { repository(repositories.map { it.url }) }"))
fun Project.repository(vararg repositories: Repository) = mavenQuick(*repositories)

/**
 * Quickly add a maven repository
 */
@Deprecated(
    "Use galaxy { repository { ... } } instead",
    ReplaceWith("galaxy { repository(repositories) }"))
fun Project.repository(vararg repositories: String): Map<String, MavenArtifactRepository> = repositories.associateWith { this.repositories.maven(it) }
