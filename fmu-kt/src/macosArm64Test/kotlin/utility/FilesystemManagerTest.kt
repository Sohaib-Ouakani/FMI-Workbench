package utility

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.*

private fun tempDir(name: String): String {
    val p = "/tmp/fsmanager_test_$name"
    SystemFileSystem.createDirectories(Path(p))
    return p
}

private fun deleteDir(path: String) {
    fun rec(p: Path) {
        if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true)
            SystemFileSystem.list(p).forEach { rec(it) }
        SystemFileSystem.delete(p)
    }
    rec(Path(path))
}

class FilesystemManagerTest {

    private val fs = FilesystemManager()

    @Test
    fun `fileExists returns false for non-existent path`() {
        assertFalse(fs.fileExists("/tmp/does_not_exist_xyz_abc"))
    }

    @Test
    fun `fileExists returns true for existing file`() {
        val tmp = tempDir("exists")
        try {
            val file = "$tmp/test.txt"
            fs.writeFile(file, "hello")
            assertTrue(fs.fileExists(file))
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `writeFile creates the file`() {
        val tmp = tempDir("write")
        try {
            val file = "$tmp/out.txt"
            fs.writeFile(file, "content")
            assertTrue(fs.fileExists(file))
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `readFile returns written content`() {
        val tmp = tempDir("readwrite")
        try {
            val file = "$tmp/data.txt"
            fs.writeFile(file, "hello world")
            assertEquals("hello world", fs.readFile(file))
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `readFile trims whitespace`() {
        val tmp = tempDir("trim")
        try {
            val file = "$tmp/padded.txt"
            fs.writeFile(file, "  trimmed  ")
            assertEquals("trimmed", fs.readFile(file))
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `readFile throws for non-existent file`() {
        assertFailsWith<IllegalStateException> {
            fs.readFile("/tmp/nope_xyz.txt")
        }
    }

    @Test
    fun `writeFile overwrites existing content`() {
        val tmp = tempDir("overwrite")
        try {
            val file = "$tmp/over.txt"
            fs.writeFile(file, "first")
            fs.writeFile(file, "second")
            assertEquals("second", fs.readFile(file))
        } finally { deleteDir(tmp) }
    }

    @Test
    fun `pathAbsolute returns path unchanged when already absolute`() {
        val abs = "/tmp/absolute/path"
        assertEquals(abs, fs.pathAbsolute(abs))
    }

    @Test
    fun `pathAbsolute prepends cwd for relative path`() {
        val result = fs.pathAbsolute("relative/path")
        assertTrue(result.startsWith("/"))
        assertTrue(result.endsWith("relative/path"))
    }
}
