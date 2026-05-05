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

class CleanUpTest {
    @Test
    fun `fmu service is closed when server stops`() = runBlocking {
        val fmu = FakeFmuService()
        val s = embeddedServer(CIO, port = 8199) {
            configureCors()
            configureRouting(FakeResourceManager(), fmu)
        }
        s.start()
        s.stop(0, 0)
        assertTrue(fmu.closeCalled)
    }
}
