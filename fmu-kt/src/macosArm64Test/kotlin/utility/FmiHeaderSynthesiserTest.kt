package utility

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private fun tempDir(name: String): String {
    val p = "/tmp/headers_test_$name"
    SystemFileSystem.createDirectories(Path(p))
    return p
}

private fun deleteDir(path: String) {
    fun rec(p: Path) {
        if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true) {
            SystemFileSystem.list(p).forEach { rec(it) }
        }
        SystemFileSystem.delete(p)
    }
    rec(Path(path))
}

class FmiHeaderSynthesiserTest {

    private val fs = FilesystemManager()
    private val synthesiser = FmiHeaderSynthesiser(fs)

    @Test
    fun `synthesise creates fmi2FunctionTypes header`() {
        val tmp = tempDir("functiontypes")
        try {
            synthesiser.synthesise(tmp)
            assertTrue(fs.fileExists("$tmp/fmi2FunctionTypes.h"))
        } finally {
            deleteDir(tmp)
        }
    }

    @Test
    fun `synthesise creates fmi2Functions header`() {
        val tmp = tempDir("functions")
        try {
            synthesiser.synthesise(tmp)
            assertTrue(fs.fileExists("$tmp/fmi2Functions.h"))
        } finally {
            deleteDir(tmp)
        }
    }

    @Test
    fun `synthesise creates fmi2TypesPlatform header`() {
        val tmp = tempDir("typesplatform")
        try {
            synthesiser.synthesise(tmp)
            assertTrue(fs.fileExists("$tmp/fmi2TypesPlatform.h"))
        } finally {
            deleteDir(tmp)
        }
    }

    @Test
    fun `fmi2TypesPlatform header contains fmi2Real typedef`() {
        val tmp = tempDir("content_real")
        try {
            synthesiser.synthesise(tmp)
            val content = fs.readFile("$tmp/fmi2TypesPlatform.h")
            assertTrue(content.contains("fmi2Real"))
        } finally {
            deleteDir(tmp)
        }
    }

    @Test
    fun `fmi2FunctionTypes header contains fmi2Status typedef`() {
        val tmp = tempDir("content_status")
        try {
            synthesiser.synthesise(tmp)
            val content = fs.readFile("$tmp/fmi2FunctionTypes.h")
            assertTrue(content.contains("fmi2Status"))
        } finally {
            deleteDir(tmp)
        }
    }

    @Test
    fun `fmi2Functions header contains fmi2Version define`() {
        val tmp = tempDir("content_version")
        try {
            synthesiser.synthesise(tmp)
            val content = fs.readFile("$tmp/fmi2Functions.h")
            assertTrue(content.contains("fmi2Version"))
        } finally {
            deleteDir(tmp)
        }
    }

    @Test
    fun `synthesise can be called multiple times without throwing`() {
        val tmp = tempDir("idempotent")
        try {
            synthesiser.synthesise(tmp)
            synthesiser.synthesise(tmp) // second call must overwrite safely
        } finally {
            deleteDir(tmp)
        }
    }
}
