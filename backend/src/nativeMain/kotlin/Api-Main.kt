import configuration.configureCors
import configuration.configureRouting
import fmu.DefaultFmuService
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import requestHandler.RequestHandler
import resources.manager.DefaultResourceManager

fun main(args: Array<String>) {
    val arg = args.firstOrNull()
    val resourceManager = DefaultResourceManager(arg)
    val fmuService = DefaultFmuService()
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
