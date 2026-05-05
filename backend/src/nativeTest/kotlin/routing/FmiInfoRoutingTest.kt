package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

// The server is started once per class via a lazy-init guard.
// fmu is reset in @BeforeTest so per-test error injection never leaks.
// NOTE: @BeforeTest/@AfterTest must NOT be suspend function on Kotlin/Native.
class FmiInfoRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private lateinit var fmu: FakeFmuService
    private var port: Int = 0

    @BeforeTest
    fun setup() {
        if (!::server.isInitialized) {
            fmu = FakeFmuService()
            server = embeddedServer(io.ktor.server.cio.CIO, port = 0) {
                configureCors()
                configureRouting(FakeResourceManager(), fmu)
            }
            server.start()
            port = runBlocking { server.engine.resolvedConnectors() }.first().port
            client = HttpClient(CIO)
        }
        fmu.reset()
    }

    private fun infoUrl() = "http://localhost:$port/fmi/info"

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `info endpoint returns 200`() = runBlocking {
        val response = client.get(infoUrl())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `info endpoint returns json content type`() = runBlocking {
        val response = client.get(infoUrl())
        assertEquals(true, response.contentType()?.match(ContentType.Application.Json))
    }

    @Test
    fun `info endpoint returns model name in body`() = runBlocking {
        val response = client.get(infoUrl())
        assertTrue(response.bodyAsText().contains("FakeModel"))
    }

    // ── fmu not loaded ────────────────────────────────────────────────────────

    @Test
    fun `info endpoint returns 500 when fmu not loaded`() = runBlocking {
        fmu.throwOnGetInfo = IllegalStateException("Cannot get info: FMU not loaded")
        val response = client.get(infoUrl())
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `info endpoint error response includes message`() = runBlocking {
        fmu.throwOnGetInfo = IllegalStateException("Cannot get info: FMU not loaded")
        val response = client.get(infoUrl())
        assertTrue(response.bodyAsText().contains("Error while getting fmu info"))
    }
}
