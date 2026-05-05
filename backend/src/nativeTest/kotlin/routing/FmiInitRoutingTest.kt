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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FmiInitRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private lateinit var rm: FakeResourceManager
    private lateinit var fmu: FakeFmuService

    @BeforeTest
    fun setup() {
        rm = FakeResourceManager()
        fmu = FakeFmuService()
        server = embeddedServer(io.ktor.server.cio.CIO, port = 8193) {
            configureCors()
            configureRouting(rm, fmu)
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
    fun `init with valid fmu paths returns 200`() = runBlocking {
        val response = client.post("http://localhost:8193/fmi/init")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `init calls fmu load with correct paths`() = runBlocking {
        client.post("http://localhost:8193/fmi/init")
        assertNotNull(fmu.loadedPaths)
        assertEquals(rm.fmuPathsToReturn.fmuPath, fmu.loadedPaths!!.fmuPath)
    }

    @Test
    fun `init with no fmu uploaded returns 400`() = runBlocking {
        rm.throwOnFmuPaths = NoSuchElementException("No FMU uploaded yet")
        val response = client.post("http://localhost:8193/fmi/init")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `init with no fmu includes error message`() = runBlocking {
        rm.throwOnFmuPaths = NoSuchElementException("No FMU uploaded yet")
        val response = client.post("http://localhost:8193/fmi/init")
        assertTrue(response.bodyAsText().contains("No FMU uploaded"))
    }

    @Test
    fun `init with fmu load failure returns 500`() = runBlocking {
        fmu.throwOnLoad = RuntimeException("FMU binary failed to load")
        val response = client.post("http://localhost:8193/fmi/init")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `init with fmu load failure includes error message`() = runBlocking {
        fmu.throwOnLoad = RuntimeException("FMU binary failed to load")
        val response = client.post("http://localhost:8193/fmi/init")
        assertTrue(response.bodyAsText().contains("Error initializing FMU"))
    }
}
