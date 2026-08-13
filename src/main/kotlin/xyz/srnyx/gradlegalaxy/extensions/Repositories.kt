package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.artifacts.ArtifactRepositoryContainer


open class Repositories {
    companion object {
        val REPOSITORIES: Repositories = Repositories()
    }

    val MAVEN_LOCAL = "MAVEN_LOCAL"
    val MAVEN_CENTRAL = ArtifactRepositoryContainer.MAVEN_CENTRAL_URL
    val SRNYX_RELEASES = "https://repo.srnyx.com/releases/"
    val SRNYX_SNAPSHOTS = "https://repo.srnyx.com/snapshots/"
    val SRNYX_PRIVATE = "https://repo.srnyx.com/private/"
    val FREYA_RELEASES = "https://repo.freya02.dev/releases/"
    val FREYA_SNAPSHOTS = "https://repo.freya02.dev/snapshots/"
    val SPIGOT = "https://hub.spigotmc.org/nexus/content/repositories/public/"
    val SPIGOT_SNAPSHOTS = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
    val PAPER = "https://repo.papermc.io/repository/maven-public/"
    val JITPACK = "https://jitpack.io/"
    val CLOJARS = "https://repo.clojars.org/"
    val DV8TION = "https://m2.dv8tion.net/releases/"
    val TRIUMPH_RELEASES = "https://repo.triumphteam.dev/releases/"
    val TRIUMPH_SNAPSHOTS = "https://repo.triumphteam.dev/snapshots/"
    val VIA_VERSION = "https://repo.viaversion.com/everything/"
    val PROTOCOL_LIB = "https://repo.dmulloy2.net/repository/public/"
    val SCARSZ = "https://nexus.scarsz.me/content/groups/public/"
    val CODE_MC = "https://repo.codemc.org/repository/maven-public/"
    val SONATYPE_RELEASES_OLD = "https://oss.sonatype.org/content/repositories/releases/"
    val SONATYPE_SNAPSHOTS_OLD = "https://oss.sonatype.org/content/repositories/snapshots/"
    val SONATYPE_RELEASES = "https://s01.oss.sonatype.org/content/repositories/releases/"
    val SONATYPE_SNAPSHOTS = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    val UMB_CRAFT = "https://nexus.umbcraft.online/repository/umbcraft-pub/"
    val PLACEHOLDER_API = "https://repo.extendedclip.com/content/repositories/placeholderapi/"
    val ALESSIO_DP = "https://repo.alessiodp.com/releases/"
    val MULTIVERSE = "https://repo.onarandombox.com/content/groups/public/"
    val EXTENDED_CLIP = "https://repo.extendedclip.com/content/repositories/public/"
    val ENGINE_HUB = "https://maven.enginehub.org/repo/"
    val REDEMPT = "https://redempt.dev/"
    val KRYPTON_RELEASES = "https://repo.kryptonmc.org/releases/"
    val KRYPTON_SNAPSHOTS = "https://repo.kryptonmc.org/snapshots/"
    val TWOL_STUDIOS = "https://ci.2lstudios.dev/plugin/repository/everything/"
    val FABRIC = "https://maven.fabricmc.net/"
    val SHEDANIEL = "https://maven.shedaniel.me/"
    val TERRAFORMERS = "https://maven.terraformersmc.com/releases/"
    val ISXANDER = "https://maven.isxander.dev/releases/"
    val DYNOMAKE = "https://maven.dynomake.it/releases/"
    val FASTSTATS_RELEASES = "https://repo.faststats.dev/releases/"
    val FASTSTATS_SNAPSHOTS = "https://repo.faststats.dev/snapshots/"
    val OKAERI_RELEASES = "https://repo.okaeri.cloud/releases/"
    val OKAERI_SNAPSHOTS = "https://repo.okaeri.cloud/snapshots/"
    val ESSENTIALS_RELEASES = "https://repo.essentialsx.net/releases/"
    val ESSENTIALS_SNAPSHOTS = "https://repo.essentialsx.net/snapshots/"
}
