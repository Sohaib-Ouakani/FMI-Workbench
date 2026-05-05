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

class HealthRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient

    @BeforeTest
    fun setup() {
        server = embeddedServer(io.ktor.server.cio.CIO, port = 8191) {
            configureCors()
            configureRouting(FakeResourceManager(), FakeFmuService())
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
    fun `health endpoint returns 200`() = runBlocking {
        val response = client.get("http://localhost:8191/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `health endpoint returns OK body`() = runBlocking {
        val response = client.get("http://localhost:8191/health")
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun `root endpoint returns 200`() = runBlocking {
        val response = client.get("http://localhost:8191/")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `root endpoint returns welcome message`() = runBlocking {
        val response = client.get("http://localhost:8191/")
        assertTrue(response.bodyAsText().isNotBlank())
    }

    @Test
    fun `unknown route returns 404`() = runBlocking {
        val response = client.get("http://localhost:8191/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
