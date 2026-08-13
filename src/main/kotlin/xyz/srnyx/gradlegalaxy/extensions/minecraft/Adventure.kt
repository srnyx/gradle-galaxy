package xyz.srnyx.gradlegalaxy.extensions.minecraft

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.extensions.DeferredActions
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Phase
import xyz.srnyx.gradlegalaxy.extensions.Repositories
import javax.inject.Inject


abstract class AdventureExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val platform = objects.newInstance(AdventurePlatformExtension::class.java, deferred)
    val text = objects.newInstance(AdventureTextExtension::class.java, deferred)
    val extra = objects.newInstance(AdventureExtraExtension::class.java, deferred)

    val api: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-api",
        configurations = listOf("implementation"),
    )
    val nbt: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-nbt",
        configurations = listOf("implementation"),
    )
    val key: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-key",
        configurations = listOf("implementation"),
    )
    val bom: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-bom",
        configurations = listOf("testImplementation"),
        platform = true,
    )
    val annotationProcessors: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-annotation-processors",
        configurations = listOf("annotationProcessor"),
    )


    fun platform(action: AdventurePlatformExtension.() -> Unit) = platform.action()
    fun text(action: AdventureTextExtension.() -> Unit) = text.action()
    fun extra(action: AdventureExtraExtension.() -> Unit) = extra.action()

    fun api(version: String, action: DependencyExtension.() -> Unit) {
        api.version = version
        api.action()
        deferred.defer(Phase.FINALIZE) { api.add(project) }
    }
    fun nbt(version: String, action: DependencyExtension.() -> Unit) {
        nbt.version = version
        nbt.action()
        deferred.defer(Phase.FINALIZE) { nbt.add(project) }
    }
    fun key(version: String, action: DependencyExtension.() -> Unit) {
        key.version = version
        key.action()
        deferred.defer(Phase.FINALIZE) { key.add(project) }
    }
    fun bom(version: String, action: DependencyExtension.() -> Unit) {
        bom.version = version
        bom.action()
        deferred.defer(Phase.FINALIZE) { bom.add(project) }
    }
    fun annotationProcessors(version: String, action: DependencyExtension.() -> Unit) {
        annotationProcessors.version = version
        annotationProcessors.action()
        deferred.defer(Phase.FINALIZE) { annotationProcessors.add(project) }
    }
}

abstract class AdventurePlatformExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
) : Repositories() {
    val api: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-api",
        configurations = listOf("implementation"),
    )
    val bukkit: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-bukkit",
        configurations = listOf("implementation"),
    )
    val bungeecord: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-bungeecord",
        configurations = listOf("implementation"),
    )
    val spongeapi: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-spongeapi",
        configurations = listOf("implementation"),
    )
    val fabric: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-fabric",
        configurations = listOf("implementation"),
    )
    val viaversion: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-viaversion",
        configurations = listOf("implementation"),
    )
    val facet: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-platform-facet",
        configurations = listOf("implementation"),
    )


    fun api(version: String, action: DependencyExtension.() -> Unit) {
        api.version = version
        api.action()
        deferred.defer(Phase.FINALIZE) { api.add(project) }
    }
    fun bukkit(version: String, action: DependencyExtension.() -> Unit) {
        bukkit.version = version
        bukkit.action()
        deferred.defer(Phase.FINALIZE) { bukkit.add(project) }
    }
    fun bungeecord(version: String, action: DependencyExtension.() -> Unit) {
        bungeecord.version = version
        bungeecord.action()
        deferred.defer(Phase.FINALIZE) { bungeecord.add(project) }
    }
    fun spongeapi(version: String, action: DependencyExtension.() -> Unit) {
        spongeapi.version = version
        spongeapi.action()
        deferred.defer(Phase.FINALIZE) { spongeapi.add(project) }
    }
    fun fabric(version: String, action: DependencyExtension.() -> Unit) {
        fabric.version = version
        fabric.action()
        deferred.defer(Phase.FINALIZE) { fabric.add(project) }
    }
    fun viaversion(version: String, action: DependencyExtension.() -> Unit) {
        viaversion.version = version
        viaversion.action()
        deferred.defer(Phase.FINALIZE) { viaversion.add(project) }
    }
    fun facet(version: String, action: DependencyExtension.() -> Unit) {
        facet.version = version
        facet.action()
        deferred.defer(Phase.FINALIZE) { facet.add(project) }
    }
}

abstract class AdventureTextExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val serializer = objects.newInstance(AdventureTextSerializerExtension::class.java, deferred)
    val logger = objects.newInstance(AdventureTextLoggerExtension::class.java, deferred)

    val minimessage: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-minimessage",
        configurations = listOf("implementation"),
    )


    fun serializer(action: AdventureTextSerializerExtension.() -> Unit) = serializer.action()
    fun logger(action: AdventureTextLoggerExtension.() -> Unit) = logger.action()

    fun minimessage(version: String, action: DependencyExtension.() -> Unit) {
        minimessage.version = version
        minimessage.action()
        deferred.defer(Phase.FINALIZE) { minimessage.add(project) }
    }
}

abstract class AdventureTextSerializerExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val implementation = objects.newInstance(AdventureTextSerializerImplementationExtension::class.java, deferred)

    val legacy: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-legacy",
        configurations = listOf("implementation"),
    )
    val bungeecord: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-bungeecord",
        configurations = listOf("implementation"),
    )
    val configurate3: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-configurate3",
        configurations = listOf("implementation"),
    )
    val configurate4: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-configurate4",
        configurations = listOf("implementation"),
    )
    val json: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-json",
        configurations = listOf("implementation"),
    )
    val gson: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-gson",
        configurations = listOf("implementation"),
    )
    val ansi: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-ansi",
        configurations = listOf("implementation"),
    )
    val plain: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-plain",
        configurations = listOf("implementation"),
    )


    fun implementation(action: AdventureTextSerializerImplementationExtension.() -> Unit) = implementation.action()

    fun legacy(version: String, action: DependencyExtension.() -> Unit) {
        legacy.version = version
        legacy.action()
        deferred.defer(Phase.FINALIZE) { legacy.add(project) }
    }
    fun bungeecord(version: String, action: DependencyExtension.() -> Unit) {
        bungeecord.version = version
        bungeecord.action()
        deferred.defer(Phase.FINALIZE) { bungeecord.add(project) }
    }
    fun configurate3(version: String, action: DependencyExtension.() -> Unit) {
        configurate3.version = version
        configurate3.action()
        deferred.defer(Phase.FINALIZE) { configurate3.add(project) }
    }
    fun configurate4(version: String, action: DependencyExtension.() -> Unit) {
        configurate4.version = version
        configurate4.action()
        deferred.defer(Phase.FINALIZE) { configurate4.add(project) }
    }
    fun json(version: String, action: DependencyExtension.() -> Unit) {
        json.version = version
        json.action()
        deferred.defer(Phase.FINALIZE) { json.add(project) }
    }
    fun gson(version: String, action: DependencyExtension.() -> Unit) {
        gson.version = version
        gson.action()
        deferred.defer(Phase.FINALIZE) { gson.add(project) }
    }
    fun ansi(version: String, action: DependencyExtension.() -> Unit) {
        ansi.version = version
        ansi.action()
        deferred.defer(Phase.FINALIZE) { ansi.add(project) }
    }
    fun plain(version: String, action: DependencyExtension.() -> Unit) {
        plain.version = version
        plain.action()
        deferred.defer(Phase.FINALIZE) { plain.add(project) }
    }
}

abstract class AdventureTextSerializerImplementationExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
) : Repositories() {
    val json: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-json-impl",
        configurations = listOf("implementation"),
    )
    val gson: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-serializer-gson-impl",
        configurations = listOf("implementation"),
    )


    fun json(version: String, action: DependencyExtension.() -> Unit) {
        json.version = version
        json.action()
        deferred.defer(Phase.FINALIZE) { json.add(project) }
    }
    fun gson(version: String, action: DependencyExtension.() -> Unit) {
        gson.version = version
        gson.action()
        deferred.defer(Phase.FINALIZE) { gson.add(project) }
    }
}

abstract class AdventureTextLoggerExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
) : Repositories() {
    val slf4j: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-text-logger-slf4j",
        configurations = listOf("implementation"),
    )


    fun slf4j(version: String, action: DependencyExtension.() -> Unit) {
        slf4j.version = version
        slf4j.action()
        deferred.defer(Phase.FINALIZE) { slf4j.add(project) }
    }
}

abstract class AdventureExtraExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
) : Repositories() {
    val kotlin: DependencyExtension = DependencyExtension(
        repositories = listOf(MAVEN_CENTRAL),
        group = "net.kyori",
        name = "adventure-extra-kotlin",
        configurations = listOf("implementation"),
    )


    fun kotlin(version: String, action: DependencyExtension.() -> Unit) {
        kotlin.version = version
        kotlin.action()
        deferred.defer(Phase.FINALIZE) { kotlin.add(project) }
    }
}
