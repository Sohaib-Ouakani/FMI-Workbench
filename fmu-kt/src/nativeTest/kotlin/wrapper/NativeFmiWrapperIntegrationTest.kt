package wrapper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import wrapper.simulation.config.SimulationConfig
import kotlin.test.*
import preprocessor.factory.createPreprocessor

private fun Path.walkTopDown(): Sequence<Path> = SystemFileSystem.list(this).asSequence().flatMap { p ->
    when {
        SystemFileSystem.metadataOrNull(p)?.isDirectory == true -> sequenceOf(p) + p.walkTopDown()
        SystemFileSystem.metadataOrNull(p)?.isRegularFile == true && p.name.endsWith("fmu") -> sequenceOf(p)
        else -> emptySequence()
    }
}

private val TEST_PATHS = Path("...").walkTopDown() + Path("src/nativeTest/resources/BouncingBall.fmu")
private const val BOUNCING_BALL_FMU = "src/nativeTest/resources/BouncingBall.fmu"

private fun tempDir(name: String): String {
    val path = "/tmp/wrapper_test_$name"
    SystemFileSystem.createDirectories(Path(path))
    SystemFileSystem.createDirectories(Path("$path/extracted"))
    SystemFileSystem.createDirectories(Path("$path/models"))
    return path
}

private fun deleteDir(path: String) {
    fun rec(p: Path) {
        if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true)
            SystemFileSystem.list(p).forEach { rec(it) }
        SystemFileSystem.delete(p)
    }
    rec(Path(path))
}

@OptIn(ExperimentalForeignApi::class)
class NativeFmiWrapperIntegrationTest {

    // ── cinterop smoke test ───────────────────────────────────────────────────

    @Test
    fun `fmi version enum is accessible via cinterop`() {
        // Verifies the cinterop binding itself is wired correctly.
        assertNotNull(libfmi.fmi_version_2_0_enu)
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @Test
    fun `wrapper initialises without throwing`() {
        val tmp = tempDir("init")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `wrapper close does not throw`() {
        val tmp = tempDir("close")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            w.close() // must not throw
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `wrapper fails with non-existent fmu path`() {
        val tmp = tempDir("badpath")
        try {
            assertFailsWith<IllegalArgumentException> {
                NativeFmiWrapper("/does/not/exist.fmu", "$tmp/extracted", "$tmp/models", createPreprocessor())
            }
        } finally { deleteDir(tmp) }
    }

    // ── getInfo ───────────────────────────────────────────────────────────────

    @Test
    fun `getInfo returns non-null model name`() {
        val tmp = tempDir("modelname")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            val info = w.getInfo()
            assertNotNull(info.modelName)
            assertTrue(info.modelName.isNotBlank())
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `getInfo variables list is not empty`() {
        val tmp = tempDir("vars")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            val info = w.getInfo()
            assertTrue(info.variables.isNotEmpty())
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `getInfo default experiment stop is positive`() {
        val tmp = tempDir("expstop")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            val info = w.getInfo()
            assertTrue(info.defaultExperimentStop > 0.0)
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `getInfo fmuKind is not blank`() {
        val tmp = tempDir("kind")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            val info = w.getInfo()
            assertNotNull(info.fmuKind)
            assertTrue(info.fmuKind.isNotBlank())
            w.close()
        } finally { deleteDir(tmp) }
    }

    // ── simulation ────────────────────────────────────────────────────────────

    @Test
    fun `simulation produces correct number of timestamps`() {
        val tmp = tempDir("timestamps")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            w.setupExperiment(SimulationConfig(startTime = 0.0, stopTime = 0.1, stepSize = 0.01))
            val result = w.executeExperiment()
            assertEquals(11, result.timestamps.size)
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `simulation result contains all fmu variables by default`() {
        val tmp1 = tempDir("allvars1")
        val tmp2 = tempDir("allvars2")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp1/extracted", "$tmp1/models", createPreprocessor())
            val varCount = w.getInfo().variables.size

            val w2 = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp2/extracted", "$tmp2/models", createPreprocessor())
            w2.setupExperiment(SimulationConfig(stopTime = 0.05))
            val result = w2.executeExperiment()
            assertEquals(varCount, result.variables.size)
            w.close()
            w2.close()
        } finally {
            deleteDir(tmp1)
            deleteDir(tmp2)
        }
    }

    @Test
    fun `simulation result variable lists match timestamp count`() {
        val tmp = tempDir("varlen")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            w.setupExperiment(SimulationConfig(startTime = 0.0, stopTime = 0.05, stepSize = 0.01))
            val result = w.executeExperiment()
            result.variables.values.forEach { values ->
                assertEquals(result.timestamps.size, values.size)
            }
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `simulation config is preserved in result`() {
        val tmp = tempDir("config")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            val config = SimulationConfig(startTime = 0.0, stopTime = 0.05, stepSize = 0.01)
            w.setupExperiment(config)
            val result = w.executeExperiment()
            assertEquals(config, result.config)
            w.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `executeExperiment throws when setup not called`() {
        val tmp = tempDir("nosetup")
        try {
            val w = NativeFmiWrapper(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models", createPreprocessor())
            assertFailsWith<IllegalStateException> {
                w.executeExperiment()
            }
            w.close()
        } finally { deleteDir(tmp) }
    }

}
