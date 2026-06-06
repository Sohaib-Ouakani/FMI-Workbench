package wrapper

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.Enabled
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import preprocessor.factory.createPreprocessor
import wrapper.simulation.config.SimulationConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import libfmi.fmi_version_2_0_enu

// ── FMU discovery ─────────────────────────────────────────────────────────────

private fun Path.walkFmus(): Sequence<Path> =
    SystemFileSystem.list(this).asSequence().flatMap { p ->
        when {
            SystemFileSystem.metadataOrNull(p)?.isDirectory == true ->
                p.walkFmus()
            SystemFileSystem.metadataOrNull(p)?.isRegularFile == true
                && p.name.endsWith(".fmu") ->
                sequenceOf(p)
            else ->
                emptySequence()
        }
    }

private val ALL_FMUS: List<Path> by lazy {
    val submoduleFmus = Path("src/nativeTest/resources/external/").walkFmus().toList() // TODO: set your submodule root
    val resourceFmus = listOf(Path("src/nativeTest/resources/BouncingBall.fmu"))
    (submoduleFmus + resourceFmus).distinct()
}

// ── FMI version guard ─────────────────────────────────────────────────────────

internal fun fmuId(fmuPath: String): String =
    fmuPath.hashCode().toUInt().toString(16)

expect fun fmuGuard(fmuPath: String): Enabled

// ── Temp directory helpers (unchanged from original) ──────────────────────────

internal fun tempDir(name: String): String {
    val path = "/tmp/wrapper_test_$name"
    SystemFileSystem.createDirectories(Path(path))
    SystemFileSystem.createDirectories(Path("$path/extracted"))
    SystemFileSystem.createDirectories(Path("$path/models"))
    return path
}

internal fun deleteDir(path: String) {
    fun rec(p: Path) {
        if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true)
            SystemFileSystem.list(p).forEach { rec(it) }
        SystemFileSystem.delete(p)
    }
    rec(Path(path))
}

expect fun openWrapper(fmuPath: String, tmp: String): NativeFmiWrapper

// ── Shared test logic (plain functions, reused across all FMUs) ───────────────

@OptIn(ExperimentalForeignApi::class)
private fun checkCinteropBinding() {
    assertNotNull(fmi_version_2_0_enu)
}

@OptIn(ExperimentalForeignApi::class)
private fun checkBadPath(tmp: String) {
    assertFailsWith<IllegalArgumentException> {
        NativeFmiWrapper("/does/not/exist.fmu", "$tmp/extracted", "$tmp/models", createPreprocessor())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun checkInitialises(fmuPath: String, tmp: String) {
    println("checkInitialises called with: $fmuPath")
    openWrapper(fmuPath, tmp).close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkClose(fmuPath: String, tmp: String) {
    openWrapper(fmuPath, tmp).close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkModelName(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    val info = w.getInfo()
    assertNotNull(info.modelName)
    assertTrue(info.modelName.isNotBlank())
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkVariablesNotEmpty(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    assertTrue(w.getInfo().variables.isNotEmpty())
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkDefaultExperimentStop(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    assertTrue(w.getInfo().defaultExperimentStop > 0.0)
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkFmuKind(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    val info = w.getInfo()
    assertNotNull(info.fmuKind)
    assertTrue(info.fmuKind.isNotBlank())
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkTimestampCount(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    w.setupExperiment(SimulationConfig(startTime = 0.0, stopTime = 0.1, stepSize = 0.01))
    assertEquals(11, w.executeExperiment().timestamps.size)
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkAllVariablesPresent(fmuPath: String, tmp1: String, tmp2: String) {
    val w1 = openWrapper(fmuPath, tmp1)
    val varCount = w1.getInfo().variables.size
    w1.close()

    val w2 = openWrapper(fmuPath, tmp2)
    w2.setupExperiment(SimulationConfig(stopTime = 0.05))
    assertEquals(varCount, w2.executeExperiment().variables.size)
    w2.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkVariableLengthsMatchTimestamps(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    w.setupExperiment(SimulationConfig(startTime = 0.0, stopTime = 0.05, stepSize = 0.01))
    val result = w.executeExperiment()
    result.variables.values.forEach { assertEquals(result.timestamps.size, it.size) }
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkConfigPreserved(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    val config = SimulationConfig(startTime = 0.0, stopTime = 0.05, stepSize = 0.01)
    w.setupExperiment(config)
    assertEquals(config, w.executeExperiment().config)
    w.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun checkExecuteThrowsWithoutSetup(fmuPath: String, tmp: String) {
    val w = openWrapper(fmuPath, tmp)
    assertFailsWith<IllegalStateException> { w.executeExperiment() }
    w.close()
}

// ── Spec ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
class FmiWrapperDynamicTest : FunSpec({

    // These two are version-independent — always run, no FMU needed
    test("cinterop: fmi version enum is accessible") {
        checkCinteropBinding()
    }

    test("lifecycle: wrapper fails with non-existent fmu path") {
        val tmp = tempDir("badpath")
        try { checkBadPath(tmp) } finally { deleteDir(tmp) }
    }

    // One test entry per (FMU × test case) in the report.
    // Guard: enabled only for FMI v2. When you add v1/v3 support,
    // update fmiVersionGuard() and these tests will start running automatically.
    for (fmu in ALL_FMUS) {
        val fmuPath = fmu.toString().also { check(it.isNotEmpty()) }
        val label = fmu.name.removeSuffix(".fmu")
        val uid = fmuId(fmuPath)

        // Capture into locals that are unambiguously per-iteration
        val capturedPath = fmuPath

        test("[$label] lifecycle: initialises without throwing")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_init")
                try { checkInitialises(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] lifecycle: close does not throw")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_close")
                try { checkClose(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] getInfo: model name is non-blank")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_modelname")
                try { checkModelName(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] getInfo: variables list is not empty")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_vars")
                try { checkVariablesNotEmpty(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] getInfo: default experiment stop is positive")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_expstop")
                try { checkDefaultExperimentStop(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] getInfo: fmuKind is not blank")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_kind")
                try { checkFmuKind(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] simulation: correct number of timestamps")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_timestamps")
                try { checkTimestampCount(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] simulation: result contains all fmu variables")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp1 = tempDir("${label}_${uid}_allvars1")
                val tmp2 = tempDir("${label}_${uid}_allvars2")
                try { checkAllVariablesPresent(capturedPath, tmp1, tmp2) }
                finally { deleteDir(tmp1); deleteDir(tmp2) }
            }

        test("[$label] simulation: variable lists match timestamp count")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_varlen")
                try { checkVariableLengthsMatchTimestamps(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] simulation: config is preserved in result")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_config")
                try { checkConfigPreserved(capturedPath, tmp) } finally { deleteDir(tmp) }
            }

        test("[$label] simulation: executeExperiment throws when setup not called")
            .config(enabledOrReasonIf = { fmuGuard(capturedPath) }) {
                val tmp = tempDir("${label}_${uid}_nosetup")
                try { checkExecuteThrowsWithoutSetup(capturedPath, tmp) } finally { deleteDir(tmp) }
            }
    }
})
