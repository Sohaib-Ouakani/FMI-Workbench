package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import requestHandler.RequestHandler

// The server is started once per class via a lazy-init guard.
// Because tests mutate rm/fmu to inject errors, those fakes are reset in
// @BeforeTest so every test starts from a clean slate without a server restart.
class FmiInitRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private lateinit var rm: FakeResourceManager
    private lateinit var fmu: FakeFmuService
    private var port: Int = 0

    @BeforeTest
    fun setup() {
        if (!::server.isInitialized) {
            rm = FakeResourceManager()
            fmu = FakeFmuService()
            server = embeddedServer(io.ktor.server.cio.CIO, port = 0) {
                configureCors()
                configureRouting(RequestHandler(rm, fmu))
            }
            server.start()
            port = runBlocking { server.engine.resolvedConnectors() }.first().port
            client = HttpClient(CIO)
        }
        rm.reset()
        fmu.reset()
    }

    @AfterTest
    fun teardown() {
        // Intentionally empty: lifecycle is managed at the class level via
        // the lazy-init guard in setup().
    }

    private fun initUrl() = "http://localhost:$port/fmi/init"

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `init with valid fmu paths returns 200`() = runBlocking {
        val response = client.post(initUrl())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `init calls fmu load with correct paths`() = runBlocking {
        client.post(initUrl())
        assertNotNull(fmu.loadedPaths)
        assertEquals(rm.fmuPathsToReturn.fmuPath, fmu.loadedPaths!!.fmuPath)
    }

    // ── no fmu uploaded ───────────────────────────────────────────────────────

    @Test
    fun `init with no fmu uploaded returns 400`() = runBlocking {
        rm.throwOnFmuPaths = NoSuchElementException("No FMU uploaded yet")
        val response = client.post(initUrl())
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `init with no fmu includes error message`() = runBlocking {
        rm.throwOnFmuPaths = NoSuchElementException("No FMU uploaded yet")
        val response = client.post(initUrl())
        assertTrue(response.bodyAsText().contains("No FMU uploaded"))
    }

    // ── fmu load failure ──────────────────────────────────────────────────────

    @Test
    fun `init with fmu load failure returns 500`() = runBlocking {
        fmu.throwOnLoad = RuntimeException("FMU binary failed to load")
        val response = client.post(initUrl())
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `init with fmu load failure includes error message`() = runBlocking {
        fmu.throwOnLoad = RuntimeException("FMU binary failed to load")
        val response = client.post(initUrl())
        assertTrue(response.bodyAsText().contains("Error initializing FMU"))
    }
}
