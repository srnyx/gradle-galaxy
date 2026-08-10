package xyz.srnyx.gradlegalaxy.extensions.setup.testing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import javax.inject.Inject


abstract class JUnitExtension @Inject constructor(
    objects: ObjectFactory
) {
    var configured: Boolean = false

    var action: Test.() -> Unit = {}

    fun action(action: Test.() -> Unit) {
        val previous = this.action
        this.action = {
            previous(this)
            action(this)
        }
    }

    fun setup(project: Project) {
        val extension: JUnitExtension = this@JUnitExtension

        project.tasks.named<Test>("test") {
            useJUnitPlatform()

            // For ByteBuddy/Mockito/MockBukkit
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")

            extension.action(this)
        }
    }
}