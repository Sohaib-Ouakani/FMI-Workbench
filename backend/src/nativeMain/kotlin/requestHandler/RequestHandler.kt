package requestHandler

import fmu.FmuService
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import logger.BackendLogger
import resources.manager.ResourceManagerService
import wrapper.simulation.config.SimulationConfig

class RequestHandler(
    private val resourceManager: ResourceManagerService,
    private val fmuService: FmuService
) {
    suspend fun init(call: RoutingCall) {
        try {
            fmuService.load(resourceManager.fmuPaths())
        } catch (e: NoSuchElementException) {
            BackendLogger.e("No FMU uploaded yet")
            call.respondText(e.message ?: "No FMU uploaded", status = HttpStatusCode.BadRequest)
            return
        } catch (e: Exception) {
            BackendLogger.e("Error initializing FMU: ${e.message}")
            call.respondText(
                "Error initializing FMU: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
            return
        }

        call.respondText("to view info about the fmu type /fmi/info")
    }

    suspend fun info(call: RoutingCall) {
        try {
            val result = fmuService.getInfo()
            call.respond(result)
        } catch (e: Exception) {
            call.respondText(
                "Error while getting fmu info: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    suspend fun upload(call: RoutingCall) {
        val header = call.request.header(HttpHeaders.ContentDisposition)
            ?: run {
                call.respondText(
                    "Missing Content-Disposition header with filename",
                    status = HttpStatusCode.BadRequest
                )
                return
            }
        val fileName = ContentDisposition.parse(header)
            .parameter(ContentDisposition.Parameters.FileName)
            ?: run {
                call.respondText(
                    "Filename not found in Content-Disposition header",
                    status = HttpStatusCode.BadRequest
                )
                return
            }
        val safeName = fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        // Reject if not .fmu (case insensitive)
        if (!safeName.lowercase().endsWith(".fmu")) {
            BackendLogger.w("Non FMU file upload attempted: $safeName")
            call.respondText(
                "Only .fmu files are allowed",
                status = HttpStatusCode.BadRequest
            )
            return
        }

        val channel: ByteReadChannel = call.receiveChannel()
        val bytes = channel.readRemaining().readByteArray()
        resourceManager.saveUpload(safeName, bytes)
        call.respondText("File '$safeName' salvato.", status = HttpStatusCode.OK)
    }

    suspend fun simulate(call: RoutingCall) {
        val info = try {
            fmuService.getInfo()
        } catch (e: Exception) {
            call.respondText(
                "Cannot simulate: FMU not loaded. Call /fmi/init first.",
                status = HttpStatusCode.BadRequest,
            )
            return
        }

        if (!info.canSimulate) {
            call.respondText(
                "Simulation is not supported for Model Exchange FMUs.",
                status = HttpStatusCode.UnprocessableEntity,
            )
            return
        }

        val config = try {
            call.receive<SimulationConfig>()
        } catch (e: Exception) {
            call.respondText(
                "Invalid simulation config: ${e.message}",
                status = HttpStatusCode.BadRequest,
            )
            return
        }

        try {
            val result = fmuService.simulate(config)
            call.respond(result)
        } catch (e: Exception) {
            BackendLogger.e("Simulation failed: ${e.message}", e)
            call.respondText(
                "Simulation failed: ${e.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }


    fun close(){
        fmuService.close()
    }
}
