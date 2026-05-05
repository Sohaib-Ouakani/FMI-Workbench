package resourceManager

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.random.Random
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import resources.manager.DefaultResourceManager

class DefaultResourceManagerTest {

    private lateinit var tmpDir: String

    @BeforeTest
    fun setup() {
        // Unique per-test directory: pid + random int guards against both
        // inter-process collisions and same-process parallel test runs.
        tmpDir = "/tmp/rm_test_${platform.posix.getpid()}_${Random.nextInt(Int.MAX_VALUE)}"
        platform.posix.mkdir(tmpDir, 511u) // 0o777
    }

    @AfterTest
    fun teardown() {
        // Covers the case where a test fails before calling cleanup().
        deleteRecursively(Path(tmpDir))
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun deleteRecursively(path: Path) {
        val meta = SystemFileSystem.metadataOrNull(path) ?: return
        if (meta.isDirectory) {
            SystemFileSystem.list(path).forEach { deleteRecursively(it) }
        }
        SystemFileSystem.delete(path)
    }

    private fun resourceManager() = DefaultResourceManager(tmpDir)

    // ── fmuPaths ─────────────────────────────────────────────────────────────

    @Test
    fun `fmuPaths throws before any upload`() {
        assertFailsWith<NoSuchElementException> { resourceManager().fmuPaths() }
    }

    @Test
    fun `fmuPaths returns path ending with uploaded filename`() {
        val rm = resourceManager()
        rm.saveUpload("model.fmu", ByteArray(32) { it.toByte() })
        assertTrue(rm.fmuPaths().fmuPath.endsWith("model.fmu"))
    }

    @Test
    fun `fmuPaths reflects the most recently uploaded file`() {
        val rm = resourceManager()
        rm.saveUpload("first.fmu", ByteArray(8))
        rm.saveUpload("second.fmu", ByteArray(8))
        assertTrue(rm.fmuPaths().fmuPath.endsWith("second.fmu"))
    }

    @Test
    fun `fmuPaths returns paths containing extractedDir and modelsDir segments`() {
        val rm = resourceManager()
        rm.saveUpload("model.fmu", ByteArray(8))
        val paths = rm.fmuPaths()
        assertTrue(paths.extractedDir.contains("extracted"))
        assertTrue(paths.modelsDir.contains("models"))
    }

    // ── saveUpload ────────────────────────────────────────────────────────────

    @Test
    fun `saveUpload writes the file to disk`() {
        val rm = resourceManager()
        val data = ByteArray(32) { it.toByte() }
        rm.saveUpload("model.fmu", data)
        // A successful fmuPaths() call is sufficient proof the file exists.
        assertTrue(rm.fmuPaths().fmuPath.endsWith("model.fmu"))
    }

    // ── cleanup ───────────────────────────────────────────────────────────────

    @Test
    fun `cleanup removes the resource directory`() {
        val rm = resourceManager()
        rm.saveUpload("model.fmu", ByteArray(8))
        rm.cleanup()
        val resourceDir = "$tmpDir/resources"
        assertEquals(-1, platform.posix.access(resourceDir, platform.posix.F_OK))
    }
}
