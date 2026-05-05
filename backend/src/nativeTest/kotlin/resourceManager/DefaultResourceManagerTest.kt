package resourceManager

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import resources.manager.DefaultResourceManager

class DefaultResourceManagerTest {

    private lateinit var tmpDir: String

    private fun deleteTempDir(path: Path) {
        val metadata = SystemFileSystem.metadataOrNull(path) ?: return
        if (metadata.isDirectory) {
            SystemFileSystem.list(path).forEach { deleteTempDir(it) }
        }
        SystemFileSystem.delete(path)
    }

    @BeforeTest
    fun setup() {
        // use a unique temp path per test run
        tmpDir = "/tmp/rm_test_${platform.posix.getpid()}"
        platform.posix.mkdir(tmpDir, 511u) // 0777
    }

    @AfterTest
    fun teardown() {
        // DefaultResourceManager.cleanup() deletes recursively,
        // but if a test fails before that, clean up manually
        deleteTempDir(Path(tmpDir))
    }

    @Test
    fun `fmuPaths throws before any upload`() {
        val rm = DefaultResourceManager(tmpDir)
        assertFailsWith<NoSuchElementException> { rm.fmuPaths() }
    }

    @Test
    fun `saveUpload writes file to disk`() {
        val rm = DefaultResourceManager(tmpDir)
        val data = ByteArray(32) { it.toByte() }
        rm.saveUpload("model.fmu", data)
        // fmuPaths should now succeed
        val paths = rm.fmuPaths()
        assertTrue(paths.fmuPath.endsWith("model.fmu"))
    }

    @Test
    fun `saveUpload replaces previous fmu`() {
        val rm = DefaultResourceManager(tmpDir)
        rm.saveUpload("first.fmu", ByteArray(8))
        rm.saveUpload("second.fmu", ByteArray(8))
        val paths = rm.fmuPaths()
        assertTrue(paths.fmuPath.endsWith("second.fmu"))
    }

    @Test
    fun `cleanup removes resource directory`() {
        val rm = DefaultResourceManager(tmpDir)
        rm.saveUpload("model.fmu", ByteArray(8))
        rm.cleanup()
        // after cleanup the resource dir should be gone
        val resourceDir = "$tmpDir/resources"
        assertEquals(-1, platform.posix.access(resourceDir, platform.posix.F_OK))
    }

    @Test
    fun `fmuPaths returns correct extractedDir and modelsDir`() {
        val rm = DefaultResourceManager(tmpDir)
        rm.saveUpload("model.fmu", ByteArray(8))
        val paths = rm.fmuPaths()
        assertTrue(paths.extractedDir.contains("extracted"))
        assertTrue(paths.modelsDir.contains("models"))
    }
}
