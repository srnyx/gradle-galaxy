package xyz.srnyx.gradlegalaxy.enums

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven

import xyz.srnyx.gradlegalaxy.annotations.Ignore


/**
 * Enum class for popular repositories. Use with [mavenQuick]
 */
@Suppress("unused")
enum class Repository(internal val url: String) {
    /**
     * [ArtifactRepositoryContainer.MAVEN_CENTRAL_URL]
     */
    MAVEN_CENTRAL(ArtifactRepositoryContainer.MAVEN_CENTRAL_URL),
    /**
     * [https://hub.spigotmc.org/nexus/content/repositories/snapshots/](https://hub.spigotmc.org/nexus/content/repositories/snapshots/)
     */
    SPIGOT("https://hub.spigotmc.org/nexus/content/repositories/snapshots/"),
    /**
     * [https://repo.papermc.io/repository/maven-public/](https://repo.papermc.io/repository/maven-public/)
     */
    PAPER("https://repo.papermc.io/repository/maven-public/"),
    /**
     * [https://jitpack.io/](https://jitpack.io/)
     */
    JITPACK("https://jitpack.io/"),
    /**
     * [https://repo.clojars.org/](https://repo.clojars.org/)
     */
    CLOJARS("https://repo.clojars.org/"),
    /**
     * [https://m2.dv8tion.net/releases/](https://m2.dv8tion.net/releases/)
     */
    DV8TION("https://m2.dv8tion.net/releases/"),
    /**
     * [https://repo.triumphteam.dev/releases/](https://repo.triumphteam.dev/releases/)
     */
    TRIUMPH_RELEASES("https://repo.triumphteam.dev/releases/"),
    /**
     * [https://repo.triumphteam.dev/snapshots/](https://repo.triumphteam.dev/snapshots/)
     */
    TRIUMPH_SNAPSHOTS("https://repo.triumphteam.dev/snapshots/"),
    /**
     * [https://repo.viaversion.com/everything/](https://repo.viaversion.com/everything/)
     */
    VIA_VERSION("https://repo.viaversion.com/everything/"),
    /**
     * [https://repo.dmulloy2.net/repository/public/](https://repo.dmulloy2.net/repository/public/)
     */
    PROTOCOL_LIB("https://repo.dmulloy2.net/repository/public/"),
    /**
     * [https://nexus.scarsz.me/content/groups/public/](https://nexus.scarsz.me/content/groups/public/)
     */
    SCARSZ("https://nexus.scarsz.me/content/groups/public/"),
    /**
     * [https://repo.codemc.org/repository/maven-public/](https://repo.codemc.org/repository/maven-public/)
     */
    CODE_MC("https://repo.codemc.org/repository/maven-public/"),
    /**
     * [https://oss.sonatype.org/content/repositories/releases/](https://oss.sonatype.org/content/repositories/releases/)
     */
    SONATYPE_RELEASES_OLD("https://oss.sonatype.org/content/repositories/releases/"),
    /**
     * [https://oss.sonatype.org/content/repositories/snapshots/](https://oss.sonatype.org/content/repositories/snapshots/)
     */
    SONATYPE_SNAPSHOTS_OLD("https://oss.sonatype.org/content/repositories/snapshots/"),
    /**
     * [https://s01.oss.sonatype.org/content/repositories/releases/](https://s01.oss.sonatype.org/content/repositories/releases/)
     */
    SONATYPE_RELEASES("https://s01.oss.sonatype.org/content/repositories/releases/"),
    /**
     * [https://s01.oss.sonatype.org/content/repositories/snapshots/](https://s01.oss.sonatype.org/content/repositories/snapshots/)
     */
    SONATYPE_SNAPSHOTS("https://s01.oss.sonatype.org/content/repositories/snapshots/"),
    /**
     * [https://nexus.umbcraft.online/repository/umbcraft-pub/](https://nexus.umbcraft.online/repository/umbcraft-pub/)
     */
    UMB_CRAFT("https://nexus.umbcraft.online/repository/umbcraft-pub/"),
    /**
     * [https://repo.extendedclip.com/content/repositories/placeholderapi/](https://repo.extendedclip.com/content/repositories/placeholderapi/)
     */
    PLACEHOLDER_API("https://repo.extendedclip.com/content/repositories/placeholderapi/"),
    /**
     * [https://repo.alessiodp.com/releases/](https://repo.alessiodp.com/releases/)
     */
    ALESSIO_DP("https://repo.alessiodp.com/releases/"),
    /**
     * [https://repo.onarandombox.com/content/groups/public/](https://repo.onarandombox.com/content/groups/public/)
     */
    MULTIVERSE("https://repo.onarandombox.com/content/groups/public/"),
    /**
     * [https://repo.extendedclip.com/content/repositories/public/](https://repo.extendedclip.com/content/repositories/public/)
     */
    EXTENDED_CLIP("https://repo.extendedclip.com/content/repositories/public/"),
    /**
     * [https://maven.enginehub.org/repo/](https://maven.enginehub.org/repo/)
     */
    ENGINE_HUB("https://maven.enginehub.org/repo/"),
    /**
     * [https://redempt.dev/](https://redempt.dev/)
     */
    REDEMPT("https://redempt.dev/"),
    /**
     * [https://repo.kryptonmc.org/releases/](https://repo.kryptonmc.org/releases/)
     */
    KRYPTON_RELEASES("https://repo.kryptonmc.org/releases/"),
    /**
     * [https://repo.kryptonmc.org/snapshots/](https://repo.kryptonmc.org/snapshots/)
     */
    KRYPTON_SNAPSHOTS("https://repo.kryptonmc.org/snapshots/"),
    /**
     * [https://ci.2lstudios.dev/plugin/repository/everything/](https://ci.2lstudios.dev/plugin/repository/everything/)
     */
    TWOL_STUDIOS("https://ci.2lstudios.dev/plugin/repository/everything/"),
    /**
     * [https://maven.fabricmc.net/](https://maven.fabricmc.net/)
     */
    FABRIC("https://maven.fabricmc.net/"),
    /**
     * [https://maven.shedaniel.me/](https://maven.shedaniel.me/)
     */
    SHEDANIEL("https://maven.shedaniel.me/"),
    /**
     * [https://maven.terraformersmc.com/releases/](https://maven.terraformersmc.com/releases/)
     */
    TERRAFORMERS("https://maven.terraformersmc.com/releases/"),
    /**
     * [https://maven.isxander.dev/releases/](https://maven.isxander.dev/releases/)
     */
    ISXANDER("https://maven.isxander.dev/releases/"),

    // Keep these at the bottom as they include other dependencies of other repositories
    /**
     * [https://repo.essentialsx.net/releases/](https://repo.essentialsx.net/releases/)
     */
    ESSENTIALS_RELEASES("https://repo.essentialsx.net/releases/"),
    /**
     * [https://repo.essentialsx.net/snapshots/](https://repo.essentialsx.net/snapshots/)
     */
    ESSENTIALS_SNAPSHOTS("https://repo.essentialsx.net/snapshots/"),
}

/**
 * Quickly add a maven repository using the [Repository] enum
 */
fun RepositoryHandler.mavenQuick(vararg repositories: Repository): Map<Repository, MavenArtifactRepository> =
    repositories.associateWith {
        maven(it.url)
    }

/**
 * Alias for [mavenQuick]
 */
@Ignore
fun RepositoryHandler.maven(vararg repositories: Repository): Map<Repository, MavenArtifactRepository> = mavenQuick(*repositories)

/**
 * Add all maven repositories in the [Repository] enum
 */
@Ignore
fun RepositoryHandler.mavenAll(vararg exclude: Repository) {
    Repository.values().filterNot(exclude::contains).forEach(::mavenQuick)
}

@Ignore
fun Project.repository(vararg repositories: Repository): Map<Repository, MavenArtifactRepository> = this.repositories.mavenQuick(*repositories)

@Ignore
fun Project.repository(vararg repositories: String): Map<String, MavenArtifactRepository> = repositories.associateWith { this.repositories.maven(it) }

@Ignore
fun Project.repositoryAll(vararg exclude: Repository) = this.repositories.mavenAll(*exclude)
