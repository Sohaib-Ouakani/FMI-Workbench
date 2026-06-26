package configuration

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import requestHandler.RequestHandler

fun Application.configureRouting(requestHandler: RequestHandler) {
    install(
        createApplicationPlugin("FmuCleanup") {
            on(MonitoringEvent(ApplicationStopped)) {
                requestHandler.close()
            }
        },
    )

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

            route("/simulate") {
                post {
                    requestHandler.simulate(call)
                }
            }
        }
    }
}
