package xyz.srnyx.gradlegalaxy.extensions

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.extensions.dependencies.DependenciesExtension
import xyz.srnyx.gradlegalaxy.extensions.setup.SetupExtension
import javax.inject.Inject


abstract class GradleGalaxyExtension @Inject constructor(
    objects: ObjectFactory
) {
    val dependencies = objects.newInstance(DependenciesExtension::class.java)
    val setup = objects.newInstance(SetupExtension::class.java)

    fun dependencies(action: DependenciesExtension.() -> Unit) = dependencies.action()
    fun setup(action: SetupExtension.() -> Unit) = setup.action()

    fun process(project: Project) {
        dependencies.process(project)
        setup.process(project, dependencies.annoyingAPI) // Must be after dependencies
    }
}

