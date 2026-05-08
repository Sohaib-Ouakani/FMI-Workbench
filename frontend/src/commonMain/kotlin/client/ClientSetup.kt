package com.example.fmi_client.client

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object ApiClient {
    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    val rawClient = HttpClient()
}

@Serializable
data class SimulationConfig(
    val startTime: Double = 0.0,
    val stopTime: Double? = null,
    val stepSize: Double = 0.01,
    val tolerance: Double? = null,
    val experimentName: String = "Default",
    val outputVariables: List<String> = emptyList(),
)

@Serializable
data class SimulationResult(
    val timestamps: List<Double>,
    val variables: Map<String, List<Double>>,
    val config: SimulationConfig,
)



suspend fun initApi(): Boolean {
    val response = ApiClient.client.post("http://localhost:8080/fmi/init")

    return response.status == HttpStatusCode.OK
}
suspend fun fetchInfo(): JsonObject {
    val initialized = initApi()
    if (!initialized) error("FMU not ready: upload an FMU first")

    val jsonString =  ApiClient.client.get("http://localhost:8080/fmi/info").bodyAsText()
    val json = Json.parseToJsonElement(jsonString)

    return json.jsonObject
}

suspend fun uploadFile(file: PlatformFile) {
    val bytes = file.readBytes()
    ApiClient.rawClient.post("http://localhost:8080/fmi/upload") {
        header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
        setBody(bytes)
        contentType(ContentType.Application.OctetStream)
    }
}

/**
 * Sends a simulation request to the backend and returns the result.
 * @throws IllegalStateException if the server returns a non-OK status.
 */
suspend fun runSimulation(config: SimulationConfig): SimulationResult {
    val response = ApiClient.client.post("http://localhost:8080/fmi/simulate") {
        contentType(ContentType.Application.Json)
        setBody(config)
    }
    if (response.status != HttpStatusCode.OK) {
        error("Simulation request failed (${response.status}): ${response.bodyAsText()}")
    }
    return response.body<SimulationResult>()
}

