package wrapper

import io.kotest.core.test.Enabled
import io.kotest.engine.TestAbortedException
import kotlinx.cinterop.ExperimentalForeignApi
import preprocessor.factory.createPreprocessor
import libfmi.fmi_import_allocate_context
import libfmi.fmi_import_free_context
import libfmi.fmi_import_get_fmi_version
import libfmi.fmi_version_2_0_enu

@OptIn(ExperimentalForeignApi::class)
private fun fmiVersionGuard(fmuPath: String): Enabled {
    val context = fmi_import_allocate_context(null)
        ?: return Enabled.disabled("Cannot allocate FMI import context – libfmi not ready?")
    return try {
        val tmpDir = tempDir("versioncheck_${fmuId(fmuPath)}")  // reuse your temp dir helper
        val version = fmi_import_get_fmi_version(context, fmuPath, tmpDir)
        //openWrapper(fmuPath, tmpDir).close()
        deleteDir(tmpDir)
        if (version == fmi_version_2_0_enu) Enabled.enabled
        else Enabled.disabled("FMI version $version not supported (only v2)")

    } catch (e: Exception) {
        if (e.message?.contains("FMU has no source code — cannot recompile on this platform") == true)
            throw TestAbortedException("FMU has no source code — cannot recompile on this platform")
        Enabled.enabled
    } finally {
        // if the library provides a free function, call it here
        fmi_import_free_context(context)
    }

    // TODO: replace with your real library call, e.g.:
    //   val version = getFmiVersion(fmuPath)
    //   return if (version == 2) Enabled.enabled
    //          else Enabled.disabled("FMI version $version not supported yet (only v2)")
}

actual fun fmuGuard(fmuPath: String): Enabled {
    return fmiVersionGuard(fmuPath)
}

actual fun openWrapper(fmuPath: String, tmp: String): NativeFmiWrapper {
    return NativeFmiWrapper(fmuPath, "$tmp/extracted", "$tmp/models", createPreprocessor())
}
