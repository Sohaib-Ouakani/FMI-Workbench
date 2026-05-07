import configuration.configureCors
import configuration.configureRouting
import fmu.DefaultFmu
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import preprocessor.factory.createPreprocessor
import requestHandler.RequestHandler
import resources.manager.DefaultResourceManager

fun main(args: Array<String>) {
    val arg = args.firstOrNull()
    val resourceManager = DefaultResourceManager(arg)
    val fmuService = DefaultFmu(createPreprocessor())
    val requestHandler = RequestHandler(resourceManager, fmuService)

    val server = embeddedServer(CIO, port = 8080) {
        configureCors()
        configureRouting(requestHandler)

        monitor.subscribe(ApplicationStopped) {
            resourceManager.cleanup()
        }
    }
    server.start(wait = true)
}
