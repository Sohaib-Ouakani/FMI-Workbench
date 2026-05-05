package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

// The server has no per-test mutable state, so it is started once for the
// entire class and torn down afterwards.  This avoids the port-rebinding race
// that occurs when @BeforeTest/@AfterTest restart it around every method.
class HealthRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private var port: Int = 0

    @BeforeTest
    suspend fun startServer() {
        server = embeddedServer(io.ktor.server.cio.CIO, port = 0) {
            configureCors()
            configureRouting(FakeResourceManager(), FakeFmuService())
        }
        server.start()
        port = server.engine.resolvedConnectors().first().port
        client = HttpClient(CIO)
    }

    @AfterTest
    fun stopServer() {
        client.close()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 0)
    }

    // ── /health ───────────────────────────────────────────────────────────────

    @Test
    fun `health endpoint returns 200`() = runBlocking {
        val response = client.get("http://localhost:$port/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `health endpoint returns OK body`() = runBlocking {
        val response = client.get("http://localhost:$port/health")
        assertEquals("OK", response.bodyAsText())
    }

    // ── / ─────────────────────────────────────────────────────────────────────

    @Test
    fun `root endpoint returns 200`() = runBlocking {
        val response = client.get("http://localhost:$port/")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `root endpoint returns welcome message`() = runBlocking {
        val response = client.get("http://localhost:$port/")
        assertTrue(response.bodyAsText().isNotBlank())
    }

    // ── unknown routes ────────────────────────────────────────────────────────

    @Test
    fun `unknown route returns 404`() = runBlocking {
        val response = client.get("http://localhost:$port/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
