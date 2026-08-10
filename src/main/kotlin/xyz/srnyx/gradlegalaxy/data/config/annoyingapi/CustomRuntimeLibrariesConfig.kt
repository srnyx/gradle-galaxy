package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.data.annoyingapi.AnnoyingMetadata
import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import javax.inject.Inject


data class CustomRuntimeLibrariesConfig(
    val runtimeLibraries: List<RuntimeLibrary> = emptyList(),
    /**
     * Null to disable processing
     */
    val processConfig: RuntimeLibrariesConfig? = RuntimeLibrariesConfig(configurations = listOf("compileOnly", "testImplementation")),
    /**
     * Null to disable runtime library enum generation
     */
    val generateRuntimeLibraryEnumConfig: GenerateRuntimeLibraryEnumConfig? = GenerateRuntimeLibraryEnumConfig(),
) {
    internal fun toExtension(): CustomRuntimeLibrariesExtension.() -> Unit = {
        processing(runtimeLibraries) {
            processConfig?.let { processConfig ->
                addRepositories = processConfig.addRepositories
                configurations = processConfig.configurations
                relocate = processConfig.relocate
            }
        }
        this@CustomRuntimeLibrariesConfig.generateRuntimeLibraryEnumConfig?.let {
            generateRuntimeLibraryEnum(it.toExtension())
        }
    }
}

abstract class CustomRuntimeLibrariesExtension @Inject constructor(
    objects: ObjectFactory
) {
    //TODO
    /**
     * Null to disable processing
     */
    val processing: RuntimeLibrariesExtension = objects.newInstance(RuntimeLibrariesExtension::class.java)
    /**
     * Null to disable runtime library enum generation
     */
    var generateRuntimeLibraryEnum: GenerateRuntimeLibraryEnumExtension = objects.newInstance(GenerateRuntimeLibraryEnumExtension::class.java)

    fun processing(libraries: List<RuntimeLibrary>, action: RuntimeLibrariesExtension.() -> Unit) {
        processing.libraries = libraries
        processing.action()
    }
    fun processing(libraries: List<RuntimeLibrary>) = processing(libraries) {}
    fun generateRuntimeLibraryEnum(action: GenerateRuntimeLibraryEnumExtension.() -> Unit) = generateRuntimeLibraryEnum.action()

    internal fun process(
        project: Project,
        annoyingMetadata: AnnoyingMetadata?
    ) {
        if (processing.libraries.isEmpty()) return

        // Process libraries
        processing.process(project)

        // Generate enum
        generateRuntimeLibraryEnum.process(project, processing.libraries, annoyingMetadata)
    }
}
