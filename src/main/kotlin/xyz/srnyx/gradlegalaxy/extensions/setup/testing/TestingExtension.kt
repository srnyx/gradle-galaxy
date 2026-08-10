package xyz.srnyx.gradlegalaxy.extensions.setup.testing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject


abstract class TestingExtension @Inject constructor(
    objects: ObjectFactory
) {
    val jUnit = objects.newInstance(JUnitExtension::class.java)

    fun jUnit(action: JUnitExtension.() -> Unit) {
        jUnit.configured = true
        jUnit.action()
    }

    fun process(project: Project) {
        if (jUnit.configured) jUnit.setup(project)
    }
}