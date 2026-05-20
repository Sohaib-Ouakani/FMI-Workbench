package wrapper.fmuData.infoManager

import cnames.structs.fmi2_import_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import libfmi.fmi2_base_type_enu_t
import libfmi.fmi2_fmu_kind_cs
import libfmi.fmi2_fmu_kind_me
import libfmi.fmi2_fmu_kind_me_and_cs
import libfmi.fmi2_fmu_kind_unknown
import libfmi.fmi2_import_free_variable_list
import libfmi.fmi2_import_get_author
import libfmi.fmi2_import_get_default_experiment_start
import libfmi.fmi2_import_get_default_experiment_step
import libfmi.fmi2_import_get_default_experiment_stop
import libfmi.fmi2_import_get_description
import libfmi.fmi2_import_get_fmu_kind
import libfmi.fmi2_import_get_model_name
import libfmi.fmi2_import_get_model_version
import libfmi.fmi2_import_get_real_variable_unit
import libfmi.fmi2_import_get_unit_name
import libfmi.fmi2_import_get_variable
import libfmi.fmi2_import_get_variable_as_real
import libfmi.fmi2_import_get_variable_base_type
import libfmi.fmi2_import_get_variable_list
import libfmi.fmi2_import_get_variable_list_size
import libfmi.fmi2_import_get_variable_name
import wrapper.fmuData.info.FmuInfo
import wrapper.fmuLifecycle.FmuLifecycleManager

/**
 * Manages extraction and processing of FMU metadata and variables.
 *
 * @property lifecycle Reference to the [FmuLifecycleManager] that manages the FMU structure.
 * @throws IllegalStateException if the FMU structure has not been initialized.
 */
@OptIn(ExperimentalForeignApi::class)
class InfoManagerFmu(private val fmiStruct: CPointer<fmi2_import_t>) {
    /**
     * Extracts complete FMU metadata including model information and variable list.
     * Queries the FMU structure for model name, description, version, default experiment parameters,
     * FMU kind (type of FMU), and enumerates all available variables.
     *
     * @return [FmuInfo] data class containing all extracted FMU metadata.
     * @throws IllegalStateException if the FMU structure is not initialized or variable extraction fails.
     */
    fun extractFmuInfo(): FmuInfo {
        val kind = when(fmi2_import_get_fmu_kind(fmiStruct)) {
            fmi2_fmu_kind_me -> "Model Exchange"
            fmi2_fmu_kind_cs -> "Co-Simulation"
            fmi2_fmu_kind_me_and_cs -> "Model Exchange and Co-Simulation"
            fmi2_fmu_kind_unknown -> "Unknown"
            else -> ""
        }
        val varList = fmi2_import_get_variable_list(fmiStruct, 0)
        val varlistSize = fmi2_import_get_variable_list_size(varList)
        val variablesMap = mutableMapOf<String, String>()

        for (i in 0 until varlistSize.toInt()) {
            val variable = fmi2_import_get_variable(varList, i.toULong())
            val name = fmi2_import_get_variable_name(variable)?.toKString().orEmpty()

            //extracts the unit of mesurement for real variables
            //if the variable is not of type real, unit is set to "None"
            val unit = if (
                fmi2_import_get_variable_base_type(variable) == fmi2_base_type_enu_t.fmi2_base_type_real
                ) {
                val realVar = fmi2_import_get_variable_as_real(variable)
                val unitPtr = fmi2_import_get_real_variable_unit(realVar)
                unitPtr?.let { fmi2_import_get_unit_name(it)?.toKString() } ?: ""
            } else "None"

            variablesMap[name] = unit
        }

        fmi2_import_free_variable_list(varList)

        return FmuInfo(
            fmi2_import_get_model_name(fmiStruct)?.toKString(),
            fmi2_import_get_description(fmiStruct)?.toKString(),
            fmi2_import_get_author(fmiStruct)?.toKString(),

            fmi2_import_get_model_version(fmiStruct)?.toKString(),
            fmi2_import_get_default_experiment_start(fmiStruct),
            fmi2_import_get_default_experiment_stop(fmiStruct),
            fmi2_import_get_default_experiment_step(fmiStruct),
            kind,
            variablesMap,
            canSimulate = kind != "Unknown" && kind != "Model Exchange"
        )
    }
}
