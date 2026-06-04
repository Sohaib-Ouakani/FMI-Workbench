package routing

import configuration.configureCors
import configuration.configureRouting
import fakes.FakeFmuService
import fakes.FakeResourceManager
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import requestHandler.RequestHandler

class CleanUpTest {

    @Test
    fun `fmu service is closed when server stops`() = runBlocking {
        val fmu = FakeFmuService()
        // port = 0 lets the OS pick a free port, avoiding any collision
        // with the other test-server classes.
        val server = embeddedServer(CIO, port = 0) {
            configureCors()
            configureRouting(requestHandler = RequestHandler(FakeResourceManager(), fmu))
        }
        server.start()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 0)
        assertTrue(fmu.closeCalled)
    }
}
