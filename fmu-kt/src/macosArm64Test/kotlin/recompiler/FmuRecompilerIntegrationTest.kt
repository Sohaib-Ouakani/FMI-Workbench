package recompiler

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

// Path is relative to the module root, where Gradle runs the test binary
private const val BOUNCING_BALL_FMU = "src/nativeTest/resources/BouncingBall.fmu"

private fun deleteDir(path: String) {
    fun rec(p: Path) {
        if (SystemFileSystem.metadataOrNull(p)?.isDirectory == true) {
            SystemFileSystem.list(p).forEach { rec(it) }
        }
        SystemFileSystem.delete(p)
    }
    if (SystemFileSystem.metadataOrNull(Path(path)) != null) rec(Path(path))
}

@OptIn(ExperimentalForeignApi::class)
class FmuRecompilerIntegrationTest {

    @Test
    fun `recompile produces output fmu file`() {
        val outDir = "/tmp/recompiler_test_output"
        val outFmu = "$outDir/BouncingBall_recompiled.fmu"
        SystemFileSystem.createDirectories(Path(outDir))
        try {
            FmuRecompiler().recompile(BOUNCING_BALL_FMU, outFmu)
            assertTrue(
                SystemFileSystem.metadataOrNull(Path(outFmu)) != null,
                "Output FMU was not created at $outFmu",
            )
        } finally {
            deleteDir(outDir)
        }
    }

    @Test
    fun `recompile output fmu is non-empty`() {
        val outDir = "/tmp/recompiler_test_size"
        val outFmu = "$outDir/BouncingBall_recompiled.fmu"
        SystemFileSystem.createDirectories(Path(outDir))
        try {
            FmuRecompiler().recompile(BOUNCING_BALL_FMU, outFmu)
            val size = SystemFileSystem.metadataOrNull(Path(outFmu))?.size ?: 0L
            assertTrue(size > 0L, "Output FMU is empty")
        } finally {
            deleteDir(outDir)
        }
    }

    @Test
    fun `recompile throws for non-existent input fmu`() {
        assertFailsWith<IllegalArgumentException> {
            FmuRecompiler().recompile("/does/not/exist.fmu", "/tmp/out.fmu")
        }
    }
}
