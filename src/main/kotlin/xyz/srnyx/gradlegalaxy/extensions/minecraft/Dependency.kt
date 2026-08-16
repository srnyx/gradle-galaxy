package xyz.srnyx.gradlegalaxy.extensions.minecraft

import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import xyz.srnyx.gradlegalaxy.annotations.Used
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories
import xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing.HangarDependency
import xyz.srnyx.gradlegalaxy.extensions.minecraft.publishing.HangarExtension
import javax.inject.Inject


abstract class MinecraftDependencyExtension @Inject constructor(
    private val objects: ObjectFactory,
) : Repositories() {
    @get:Input @get:Optional
    val universalDependencies: ListProperty<UniversalDependency> = objects.listProperty(UniversalDependency::class.java)

    @Used
    fun optional(action: UniversalDependency.() -> Unit) = universalDependency(false, action)

    @Used
    fun required(action: UniversalDependency.() -> Unit) = universalDependency(true, action)

    private fun universalDependency(required: Boolean, action: UniversalDependency.() -> Unit) {
        val dependency = objects.newInstance(UniversalDependency::class.java)
        dependency.required.set(required)
        dependency.action()
        universalDependencies.add(dependency)
    }

    internal fun setup(
        project: Project,
        pluginYml: PluginYmlExtension,
    ) {
        universalDependencies.orNull?.forEach { dependency ->
            dependency.add(project)
            dependency.addPluginYml(pluginYml)
        }
    }
}

abstract class UniversalDependency @Inject constructor(
    objects: ObjectFactory,
): DependencyExtension(objects) {
    @get:Input
    val required: Property<Boolean> = objects.property(Boolean::class.java)
    @get:Input @get:Optional
    val pluginYml: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val modrinth: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val curseforge: Property<String> = objects.property(String::class.java)
    @get:Input @get:Optional
    val hangar: Property<String> = objects.property(String::class.java)


    internal fun addPluginYml(pluginYmlExtension: PluginYmlExtension) {
        if (pluginYml.orNull == null) return
        pluginYmlExtension.apply { (if (required.get()) depend else softDepend).add(pluginYml.get()) }
    }

    internal fun addModrinth(modrinthTask: Modrinth) {
        if (modrinth.orNull == null) return
        modrinthTask.apply { if (required.get()) requires(modrinth.get()) else optional(modrinth.get()) }
    }

    internal fun addCurseforge(curseforgeTask: Curseforge) {
        if (curseforge.orNull == null) return
        curseforgeTask.apply { if (required.get()) requires(curseforge.get()) else optional(curseforge.get()) }
    }

    internal fun addHangar(hangarExtension: HangarExtension) {
        if (hangar.orNull == null) return
        hangarExtension.dependencies += HangarDependency(hangar.get(), required.get())
    }
}
