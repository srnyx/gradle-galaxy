package xyz.srnyx.gradlegalaxy.data.config.annoyingapi

import xyz.srnyx.gradlegalaxy.data.annoyingapi.RuntimeLibrary


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
)
