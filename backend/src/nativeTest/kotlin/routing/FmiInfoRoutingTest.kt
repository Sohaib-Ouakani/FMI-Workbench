package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FmiInfoRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private lateinit var fmu: FakeFmuService

    @BeforeTest
    fun setup() {
        fmu = FakeFmuService()
        server = embeddedServer(io.ktor.server.cio.CIO, port = 8194) {
            configureCors()
            configureRouting(FakeResourceManager(), fmu)
        }
        server.start()
        client = HttpClient(io.ktor.client.engine.cio.CIO)
    }

    @AfterTest
    fun teardown() {
        client.close()
        server.stop(0, 0)
    }

    @Test
    fun `info endpoint returns 200`() = runBlocking {
        val response = client.get("http://localhost:8194/fmi/info")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `info endpoint returns json content type`() = runBlocking {
        val response = client.get("http://localhost:8194/fmi/info")
        assertEquals(response.contentType()?.match(ContentType.Application.Json), true)
    }

    @Test
    fun `info endpoint returns model name in body`() = runBlocking {
        val response = client.get("http://localhost:8194/fmi/info")
        assertTrue(response.bodyAsText().contains("FakeModel"))
    }

    @Test
    fun `info endpoint returns 500 when fmu not loaded`() = runBlocking {
        fmu.throwOnGetInfo = IllegalStateException("Cannot get info: FMU not loaded")
        val response = client.get("http://localhost:8194/fmi/info")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `info endpoint error response includes message`() = runBlocking {
        fmu.throwOnGetInfo = IllegalStateException("Cannot get info: FMU not loaded")
        val response = client.get("http://localhost:8194/fmi/info")
        assertTrue(response.bodyAsText().contains("Error while getting fmu info"))
    }
}
