package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import requestHandler.RequestHandler

private const val BODY_SIZE = 64

// The server is started once per class.  Because individual tests assert on
// FakeResourceManager state (lastSavedFileName, etc.), the fake is reset in
// @BeforeTest without restarting the server.
class UploadRoutingTest {

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    private lateinit var client: HttpClient
    private lateinit var rm: FakeResourceManager
    private var port: Int = 0

    @BeforeTest
    fun setup() {
        if (!::server.isInitialized) {
            rm = FakeResourceManager()
            server = embeddedServer(io.ktor.server.cio.CIO, port = 0) {
                configureCors()
                configureRouting(RequestHandler(rm, FakeFmuService()))
            }
            server.start()
            port = runBlocking { server.engine.resolvedConnectors() }.first().port
            client = HttpClient(CIO)
        }
        rm.reset()
    }

    // Helper so every test shares the same upload URL without repeating the port.
    private fun uploadUrl() = "http://localhost:$port/fmi/upload"

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `upload valid fmu returns 200`() = runBlocking {
        val response = client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"BouncingBall.fmu\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE) { it.toByte() })
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `upload valid fmu saves correct filename`() = runBlocking {
        client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"BouncingBall.fmu\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE) { it.toByte() })
        }
        assertEquals("BouncingBall.fmu", rm.lastSavedFileName)
    }

    @Test
    fun `upload valid fmu saves correct bytes`() = runBlocking {
        val payload = ByteArray(BODY_SIZE) { it.toByte() }
        client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"BouncingBall.fmu\"")
            contentType(ContentType.Application.OctetStream)
            setBody(payload)
        }
        assertNotNull(rm.lastSavedData)
        assertEquals(payload.size, rm.lastSavedData!!.size)
    }

    @Test
    fun `upload fmu with uppercase extension returns 200`() = runBlocking {
        val response = client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"Model.FMU\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ── missing / malformed Content-Disposition ───────────────────────────────

    @Test
    fun `upload missing Content-Disposition header returns 400`() = runBlocking {
        val response = client.post(uploadUrl()) {
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload missing Content-Disposition includes error message`() = runBlocking {
        val response = client.post(uploadUrl()) {
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertTrue(response.bodyAsText().contains("Missing Content-Disposition"))
    }

    @Test
    fun `upload header without filename returns 400`() = runBlocking {
        val response = client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload header without filename includes error message`() = runBlocking {
        val response = client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertTrue(response.bodyAsText().contains("Filename not found"))
    }

    // ── wrong file type ───────────────────────────────────────────────────────

    @Test
    fun `upload non-fmu file returns 400`() = runBlocking {
        val response = client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"model.zip\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload non-fmu file includes error message`() = runBlocking {
        val response = client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"model.zip\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertTrue(response.bodyAsText().contains("Only .fmu files are allowed"))
    }

    // ── filename sanitisation ─────────────────────────────────────────────────

    @Test
    fun `upload sanitizes path traversal characters in filename`() = runBlocking {
        client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"../evil/path.fmu\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertEquals(".._evil_path.fmu", rm.lastSavedFileName)
    }

    @Test
    fun `upload sanitizes all dangerous characters`() = runBlocking {
        client.post(uploadUrl()) {
            header(HttpHeaders.ContentDisposition, "attachment; filename=\"my:model*.fmu\"")
            contentType(ContentType.Application.OctetStream)
            setBody(ByteArray(BODY_SIZE))
        }
        assertEquals("my_model_.fmu", rm.lastSavedFileName)
    }
}
