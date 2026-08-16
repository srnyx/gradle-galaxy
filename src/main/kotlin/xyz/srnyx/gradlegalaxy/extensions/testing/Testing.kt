package xyz.srnyx.gradlegalaxy.extensions.testing

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import xyz.srnyx.gradlegalaxy.extensions.DeferredActions
import xyz.srnyx.gradlegalaxy.extensions.DependencyExtension
import xyz.srnyx.gradlegalaxy.extensions.Repositories.Companion.REPOSITORIES
import xyz.srnyx.gradlegalaxy.extensions.Phase
import javax.inject.Inject


abstract class TestingExtension @Inject internal constructor(
    private val project: Project,
    private val deferred: DeferredActions,
    objects: ObjectFactory
) {
    val jUnit = objects.newInstance(JUnitExtension::class.java)
    val mockBukkit = objects.newInstance(MockBukkitExtension::class.java)

    fun jUnit(version: String, action: JUnitExtension.() -> Unit = {}) {
        jUnit.version.set(version)
        jUnit.action()
        deferred.defer(Phase.WIRE) { jUnit.setup(project) }
        deferred.defer(Phase.FINALIZE) {
            // Add other required dependencies
            project.dependencies {
                add("testImplementation", "org.junit.jupiter:junit-jupiter")
                add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
            }

            // Add JUnit BOM
            jUnit.add(project)
        }
    }

    fun mockBukkit(version: String, action: MockBukkitExtension.() -> Unit = {}) {
        mockBukkit.version.set(version)
        mockBukkit.action()
        deferred.defer(Phase.FINALIZE) { mockBukkit.add(project) }
    }
}

abstract class JUnitExtension @Inject constructor(
    objects: ObjectFactory,
) : DependencyExtension(objects) {
    init { apply {
        repositories.set(listOf(REPOSITORIES.MAVEN_CENTRAL))
        group.set("org.junit")
        name.set("junit-bom")
        configurations.set(listOf("testImplementation"))
        platform.set(true)
    } }
    var testAction: Test.() -> Unit = {}

    fun testAction(testAction: Test.() -> Unit) {
        val previous = this.testAction
        this.testAction = {
            previous(this)
            testAction(this)
        }
    }

    internal fun setup(project: Project) {
        val extension: JUnitExtension = this@JUnitExtension

        project.tasks.named<Test>("test") {
            useJUnitPlatform()

            // For ByteBuddy/Mockito/MockBukkit
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")

            extension.testAction(this)
        }
    }
}
