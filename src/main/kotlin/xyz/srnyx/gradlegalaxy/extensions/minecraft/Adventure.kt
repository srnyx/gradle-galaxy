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

    val api: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-api")
        configurations.set(listOf("implementation"))
    }
    val nbt: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-nbt")
        configurations.set(listOf("implementation"))
    }
    val key: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-key")
        configurations.set(listOf("implementation"))
    }
    val bom: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-bom")
        configurations.set(listOf("testImplementation"))
        platform.set(true)
    }
    val annotationProcessors: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-annotation-processors")
        configurations.set(listOf("annotationProcessor"))
    }


    fun platform(action: AdventurePlatformExtension.() -> Unit) = platform.action()
    fun text(action: AdventureTextExtension.() -> Unit) = text.action()
    fun extra(action: AdventureExtraExtension.() -> Unit) = extra.action()

    fun api(version: String, action: DependencyExtension.() -> Unit) {
        api.version.set(version)
        api.action()
        deferred.defer(Phase.FINALIZE) { api.add(project) }
    }
    fun nbt(version: String, action: DependencyExtension.() -> Unit) {
        nbt.version.set(version)
        nbt.action()
        deferred.defer(Phase.FINALIZE) { nbt.add(project) }
    }
    fun key(version: String, action: DependencyExtension.() -> Unit) {
        key.version.set(version)
        key.action()
        deferred.defer(Phase.FINALIZE) { key.add(project) }
    }
    fun bom(version: String, action: DependencyExtension.() -> Unit) {
        bom.version.set(version)
        bom.action()
        deferred.defer(Phase.FINALIZE) { bom.add(project) }
    }
    fun annotationProcessors(version: String, action: DependencyExtension.() -> Unit) {
        annotationProcessors.version.set(version)
        annotationProcessors.action()
        deferred.defer(Phase.FINALIZE) { annotationProcessors.add(project) }
    }
}

abstract class AdventurePlatformExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val api: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-api")
        configurations.set(listOf("implementation"))
    }
    val bukkit: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-bukkit")
        configurations.set(listOf("implementation"))
    }
    val bungeecord: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-bungeecord")
        configurations.set(listOf("implementation"))
    }
    val spongeapi: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-spongeapi")
        configurations.set(listOf("implementation"))
    }
    val fabric: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-fabric")
        configurations.set(listOf("implementation"))
    }
    val viaversion: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-viaversion")
        configurations.set(listOf("implementation"))
    }
    val facet: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-platform-facet")
        configurations.set(listOf("implementation"))
    }


    fun api(version: String, action: DependencyExtension.() -> Unit) {
        api.version.set(version)
        api.action()
        deferred.defer(Phase.FINALIZE) { api.add(project) }
    }
    fun bukkit(version: String, action: DependencyExtension.() -> Unit) {
        bukkit.version.set(version)
        bukkit.action()
        deferred.defer(Phase.FINALIZE) { bukkit.add(project) }
    }
    fun bungeecord(version: String, action: DependencyExtension.() -> Unit) {
        bungeecord.version.set(version)
        bungeecord.action()
        deferred.defer(Phase.FINALIZE) { bungeecord.add(project) }
    }
    fun spongeapi(version: String, action: DependencyExtension.() -> Unit) {
        spongeapi.version.set(version)
        spongeapi.action()
        deferred.defer(Phase.FINALIZE) { spongeapi.add(project) }
    }
    fun fabric(version: String, action: DependencyExtension.() -> Unit) {
        fabric.version.set(version)
        fabric.action()
        deferred.defer(Phase.FINALIZE) { fabric.add(project) }
    }
    fun viaversion(version: String, action: DependencyExtension.() -> Unit) {
        viaversion.version.set(version)
        viaversion.action()
        deferred.defer(Phase.FINALIZE) { viaversion.add(project) }
    }
    fun facet(version: String, action: DependencyExtension.() -> Unit) {
        facet.version.set(version)
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

    val minimessage: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-minimessage")
        configurations.set(listOf("implementation"))
    }


    fun serializer(action: AdventureTextSerializerExtension.() -> Unit) = serializer.action()
    fun logger(action: AdventureTextLoggerExtension.() -> Unit) = logger.action()

    fun minimessage(version: String, action: DependencyExtension.() -> Unit) {
        minimessage.version.set(version)
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

    val legacy: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-legacy")
        configurations.set(listOf("implementation"))
    }
    val bungeecord: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-bungeecord")
        configurations.set(listOf("implementation"))
    }
    val configurate3: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-configurate3")
        configurations.set(listOf("implementation"))
    }
    val configurate4: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-configurate4")
        configurations.set(listOf("implementation"))
    }
    val json: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-json")
        configurations.set(listOf("implementation"))
    }
    val gson: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-gson")
        configurations.set(listOf("implementation"))
    }
    val ansi: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-ansi")
        configurations.set(listOf("implementation"))
    }
    val plain: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-plain")
        configurations.set(listOf("implementation"))
    }


    fun implementation(action: AdventureTextSerializerImplementationExtension.() -> Unit) = implementation.action()

    fun legacy(version: String, action: DependencyExtension.() -> Unit) {
        legacy.version.set(version)
        legacy.action()
        deferred.defer(Phase.FINALIZE) { legacy.add(project) }
    }
    fun bungeecord(version: String, action: DependencyExtension.() -> Unit) {
        bungeecord.version.set(version)
        bungeecord.action()
        deferred.defer(Phase.FINALIZE) { bungeecord.add(project) }
    }
    fun configurate3(version: String, action: DependencyExtension.() -> Unit) {
        configurate3.version.set(version)
        configurate3.action()
        deferred.defer(Phase.FINALIZE) { configurate3.add(project) }
    }
    fun configurate4(version: String, action: DependencyExtension.() -> Unit) {
        configurate4.version.set(version)
        configurate4.action()
        deferred.defer(Phase.FINALIZE) { configurate4.add(project) }
    }
    fun json(version: String, action: DependencyExtension.() -> Unit) {
        json.version.set(version)
        json.action()
        deferred.defer(Phase.FINALIZE) { json.add(project) }
    }
    fun gson(version: String, action: DependencyExtension.() -> Unit) {
        gson.version.set(version)
        gson.action()
        deferred.defer(Phase.FINALIZE) { gson.add(project) }
    }
    fun ansi(version: String, action: DependencyExtension.() -> Unit) {
        ansi.version.set(version)
        ansi.action()
        deferred.defer(Phase.FINALIZE) { ansi.add(project) }
    }
    fun plain(version: String, action: DependencyExtension.() -> Unit) {
        plain.version.set(version)
        plain.action()
        deferred.defer(Phase.FINALIZE) { plain.add(project) }
    }
}

abstract class AdventureTextSerializerImplementationExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val json: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-json-impl")
        configurations.set(listOf("implementation"))
    }
    val gson: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-serializer-gson-impl")
        configurations.set(listOf("implementation"))
    }


    fun json(version: String, action: DependencyExtension.() -> Unit) {
        json.version.set(version)
        json.action()
        deferred.defer(Phase.FINALIZE) { json.add(project) }
    }
    fun gson(version: String, action: DependencyExtension.() -> Unit) {
        gson.version.set(version)
        gson.action()
        deferred.defer(Phase.FINALIZE) { gson.add(project) }
    }
}

abstract class AdventureTextLoggerExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val slf4j: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-text-logger-slf4j")
        configurations.set(listOf("implementation"))
    }


    fun slf4j(version: String, action: DependencyExtension.() -> Unit) {
        slf4j.version.set(version)
        slf4j.action()
        deferred.defer(Phase.FINALIZE) { slf4j.add(project) }
    }
}

abstract class AdventureExtraExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory,
) : Repositories() {
    val kotlin: DependencyExtension = DependencyExtension(objects).apply {
        repositories.set(listOf(MAVEN_CENTRAL))
        group.set("net.kyori")
        name.set("adventure-extra-kotlin")
        configurations.set(listOf("implementation"))
    }


    fun kotlin(version: String, action: DependencyExtension.() -> Unit) {
        kotlin.version.set(version)
        kotlin.action()
        deferred.defer(Phase.FINALIZE) { kotlin.add(project) }
    }
}
