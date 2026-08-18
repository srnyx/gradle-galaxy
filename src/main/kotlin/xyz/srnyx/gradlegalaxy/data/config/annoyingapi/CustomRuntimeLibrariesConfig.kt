package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary
import xyz.srnyx.gradlegalaxy.extensions.minecraft.CustomRuntimeLibrariesExtension


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
        addDataLibraries(runtimeLibraries)
        processConfig?.let { processConfig ->
            configurations.set(processConfig.configurations)
            relocate.set(processConfig.relocate)
        }
        this@CustomRuntimeLibrariesConfig.generateRuntimeLibraryEnumConfig?.let {
            generateRuntimeLibraryEnum(it.toExtension())
        }
    }
}
