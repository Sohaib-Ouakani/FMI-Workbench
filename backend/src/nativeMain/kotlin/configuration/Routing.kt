package configuration

import io.ktor.server.application.*
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import requestHandler.RequestHandler

fun Application.configureRouting(requestHandler: RequestHandler) {

    install(createApplicationPlugin("FmuCleanup") {
        on(MonitoringEvent(ApplicationStopped)) {
            requestHandler.close()
        }
    })

    routing {
        get("/health") { call.respondText("OK") }
        get("/") { call.respondText("Welcome to the home of the api") }

        route("/fmi") {
            install(ContentNegotiation) { json() }
            route("/init") {
                post {
                    requestHandler.init(call)
                }
            }

            route("/info") {
                get {
                    requestHandler.info(call)
                }
            }

            post("/upload") {
                requestHandler.upload(call)
            }
        }
    }
}
