package wrapper.fmuLifecycle

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import libfmi.fmi2_fmu_kind_cs
import libfmi.fmi2_fmu_kind_me
import libfmi.fmi2_fmu_kind_me_and_cs
import libfmi.fmi2_import_create_dllfmu
import libfmi.fmi2_import_destroy_dllfmu
import libfmi.fmi2_import_free
import libfmi.fmi2_import_free_instance
import libfmi.fmi2_import_get_fmu_kind
import libfmi.fmi2_import_parse_xml
import libfmi.fmi2_import_terminate
import libfmi.fmi_import_allocate_context
import libfmi.fmi_import_context_t
import libfmi.fmi_import_free_context
import libfmi.fmi_import_get_fmi_version
import libfmi.fmi_version_2_0_enu
import logger.FmuKtLogger

private enum class DLLSTATUS {
    /**
     * Indicates successful FMU DLL loading.
     */
    OK,

    /**
     * Indicates failure during FMU DLL loading.
     */
    ERROR,
}

/**
 * Manages the complete lifecycle of an FMU from loading through cleanup.
 * Handles FMI context creation, XML parsing, DLL loading, and resource deallocation.
 * Responsible for initializing FMILib and detecting FMU capabilities (Model Exchange vs Co-Simulation).
 *
 * @property fmuFile The resolved path to the FMU file.
 * @property unpackDir The directory where FMU contents will be extracted.
 * @property context FMILib context pointer, null if creation failed.
 * @property fmiStruct Parsed FMI structure, null until [start] is called.
 * @property canSimulate Boolean flag indicating if the FMU supports Co-Simulation (true if not Model Exchange only).
 * @throws IllegalStateException if XML parsing fails or DLL loading encounters an error.
 */
@OptIn(ExperimentalForeignApi::class)
class FmuLifecycleManager(val fmuFile: String, val unpackDir: String) {
    private val context: CPointer<fmi_import_context_t>? = fmi_import_allocate_context(null)
    private var fmiStruct: CPointer<cnames.structs.fmi2_import_t>? = null

    /**
     * Indicates whether the FMU can be simulated. True for Co-Simulation FMUs, false for Model Exchange only.
     */
    var canSimulate: Boolean = false

    init {
        fmi_import_get_fmi_version(this.context, fmuFile, unpackDir)
    }

    /**
     * Initializes the FMU lifecycle by parsing XML and loading the DLL.
     * Must be called before any operation on the FMU.
     * Determines the FMU type and checks if simulation is supported.
     *
     * @throws IllegalStateException if XML parsing fails or the FMU binary cannot be loaded.
     */
    fun start() {
        fmiStruct = fmi2_import_parse_xml(context, unpackDir, null)
        requireNotNull(fmiStruct) {
            "Failed to parse XML. The FMU may be corrupt or unsupported."
        }

        val fmuKind = fmi2_import_get_fmu_kind(fmiStruct)

        if (fmuKind != fmi2_fmu_kind_cs && fmuKind != fmi2_fmu_kind_me_and_cs) {
            FmuKtLogger.i("FMU is of type Model Exchange. Simulation not supported.")
        } else {
            canSimulate = true
        }

        val dllResult = fmi2_import_create_dllfmu(fmiStruct, fmi_version_2_0_enu, null)
        val dllStatus = if (dllResult == 0) DLLSTATUS.OK else DLLSTATUS.ERROR

        if (dllStatus == DLLSTATUS.ERROR) {
            throw IllegalStateException("Error when loading FMU binary")
        }
    }

    /**
     * Terminates and releases all FMU resources including the FMI structure and context.
     * Should be called when the FMU is no longer needed to prevent memory leaks.
     *
     * @throws IllegalStateException if an error occurs during resource cleanup.
     */
    fun close() {
        val s = fmiStruct ?: return
        fmi2_import_destroy_dllfmu(s)
        fmi2_import_free(s)
        fmiStruct = null
        fmi_import_free_context(context)
    }

    fun requireFmiStruct(): CPointer<cnames.structs.fmi2_import_t> =
        fmiStruct ?: throw IllegalStateException("FMU not started")
}
