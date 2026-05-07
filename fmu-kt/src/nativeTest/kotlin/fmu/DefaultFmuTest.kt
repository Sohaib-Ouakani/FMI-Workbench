package fmu

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import wrapper.simulation.config.SimulationConfig
import kotlin.test.*
import preprocessor.factory.createPreprocessor

// Helper: resolve path to the BouncingBall.fmu bundled in test resources.
// The test binary working directory is the module root when run via Gradle.
private const val BOUNCING_BALL_FMU = "src/nativeTest/resources/BouncingBall.fmu"

private fun tempDir(name: String): String {
    val path = "/tmp/defaultfmu_test_$name"
    SystemFileSystem.createDirectories(Path(path))
    SystemFileSystem.createDirectories(Path("$path/extracted"))
    SystemFileSystem.createDirectories(Path("$path/models"))
    return path
}

private fun deleteDir(path: String) {
    val p = Path(path)
    fun rec(p: Path) {
        if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true)
            SystemFileSystem.list(p).forEach { rec(it) }
        SystemFileSystem.delete(p)
    }
    rec(p)
}

class DefaultFmuTest {

    // ── Unit tests (no real FMU needed) ──────────────────────────────────────

    @Test
    fun `getInfo throws when fmu not loaded`() {
        val fmu = DefaultFmu(createPreprocessor())
        assertFailsWith<IllegalStateException> {
            fmu.getInfo()
        }
    }

    @Test
    fun `simulate throws when fmu not loaded`() {
        val fmu = DefaultFmu(createPreprocessor())
        assertFailsWith<IllegalStateException> {
            fmu.simulate(SimulationConfig())
        }
    }

    @Test
    fun `close on unloaded fmu does not throw`() {
        val fmu = DefaultFmu(createPreprocessor())
        fmu.close() // must be safe to call even before load
    }

    @Test
    fun `close can be called multiple times safely`() {
        val fmu = DefaultFmu(createPreprocessor())
        fmu.close()
        fmu.close()
    }

    // ── Integration tests (require BouncingBall.fmu) ──────────────────────────

    @Test
    fun `load succeeds with valid fmu`() {
        val tmp = tempDir("load")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(
                fmuPath = BOUNCING_BALL_FMU,
                extractedDir = "$tmp/extracted",
                modelsDir = "$tmp/models"
            ))
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `getInfo returns non-null model name after load`() {
        val tmp = tempDir("getinfo")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models"))
            val info = fmu.getInfo()
            assertNotNull(info.modelName)
            assertTrue(info.modelName.isNotBlank())
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `getInfo returns variables list`() {
        val tmp = tempDir("variables")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models"))
            val info = fmu.getInfo()
            assertTrue(info.variables.isNotEmpty())
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `getInfo returns co-simulation fmu kind`() {
        val tmp = tempDir("kind")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models"))
            val info = fmu.getInfo()
            assertNotNull(info.fmuKind)
            assertTrue(info.fmuKind.isNotBlank())
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `load twice closes previous wrapper cleanly`() {
        val tmp = tempDir("reload")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            val paths = FmuPaths(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models")
            fmu.load(paths)
            fmu.load(paths) // second load must not throw
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `simulate returns timestamps matching step count`() {
        val tmp = tempDir("simulate")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models"))
            val config = SimulationConfig(
                startTime = 0.0,
                stopTime = 0.1,
                stepSize = 0.01
            )
            val result = fmu.simulate(config)
            // 0.0, 0.01, 0.02 … 0.10 → 11 steps
            assertEquals(11, result.timestamps.size)
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `simulate timestamps start at configured start time`() {
        val tmp = tempDir("timestamps")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp/extracted", "$tmp/models"))
            val result = fmu.simulate(SimulationConfig(startTime = 0.0, stopTime = 0.05, stepSize = 0.01))
            assertEquals(0.0, result.timestamps.first(), 1e-9)
            fmu.close()
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `simulate result variables match requested output variables`() {
        val tmp1 = tempDir("outvars1")
        val tmp2 = tempDir("outvars2")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp1/extracted", "$tmp1/models"))
            val info = fmu.getInfo()
            // reload needed after getInfo teardown (simulate frees the instance)
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp2/extracted", "$tmp2/models"))
            val targetVar = info.variables.first()
            val result = fmu.simulate(SimulationConfig(
                stopTime = 0.05,
                outputVariables = listOf(targetVar)
            ))
            assertTrue(result.variables.containsKey(targetVar))
            assertEquals(1, result.variables.size)
            fmu.close()
        } finally {
            deleteDir(tmp1)
            deleteDir(tmp2)
        }
    }

    @Test
    fun `simulate with no output variables records all fmu variables`() {
        val tmp1 = tempDir("allvars")
        val tmp2 = tempDir("allvars2")
        try {
            val fmu = DefaultFmu(createPreprocessor())
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp1/extracted", "$tmp1/models"))
            val info = fmu.getInfo()
            fmu.load(FmuPaths(BOUNCING_BALL_FMU, "$tmp2/extracted", "$tmp2/models"))
            val result = fmu.simulate(SimulationConfig(stopTime = 0.05))
            assertEquals(info.variables.size, result.variables.size)
            fmu.close()
        } finally {
            deleteDir(tmp1)
            deleteDir(tmp2)
        }
    }
}
