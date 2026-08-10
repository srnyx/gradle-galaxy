package xyz.srnyx.gradlegalaxy.extensions.setup

import org.gradle.api.model.ObjectFactory
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupExtension
import xyz.srnyx.gradlegalaxy.data.config.JdaSetupExtension
import xyz.srnyx.gradlegalaxy.data.config.MCSetupExtension
import xyz.srnyx.gradlegalaxy.data.config.annoyingapi.AnnoyingSetupExtension
import xyz.srnyx.gradlegalaxy.extensions.setup.publishing.PublishingExtension
import xyz.srnyx.gradlegalaxy.extensions.setup.testing.TestingExtension
import javax.inject.Inject


abstract class SetupExtension @Inject constructor(
    objects: ObjectFactory
) {
    val publishing: PublishingExtension = objects.newInstance(PublishingExtension::class.java)
    val testing = objects.newInstance(TestingExtension::class.java)

    val java = objects.newInstance(JavaSetupExtension::class.java)
    val minecraft = objects.newInstance(MCSetupExtension::class.java)
    val annoyingAPI = objects.newInstance(AnnoyingSetupExtension::class.java)
    val runPaper = objects.newInstance(RunPaperExtension::class.java)
    val jda = objects.newInstance(JdaSetupExtension::class.java)

    fun publishing(action: PublishingExtension.() -> Unit) = publishing.action()
    fun testing(action: TestingExtension.() -> Unit) = testing.action()

    fun java(action: JavaSetupExtension.() -> Unit) {
        java.configured = true
        java.action()
    }
    fun minecraft(action: MCSetupExtension.() -> Unit) {
        minecraft.configured = true
        minecraft.action()
    }
    fun annoyingAPI(action: AnnoyingSetupExtension.() -> Unit) {
        annoyingAPI.configured = true
        annoyingAPI.action()
    }
    fun runPaper(action: RunPaperExtension.() -> Unit) {
        runPaper.configured = true
        runPaper.action()
    }
    fun jda(action: JdaSetupExtension.() -> Unit) {
        jda.configured = true
        jda.action()
    }
}
