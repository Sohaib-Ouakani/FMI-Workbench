package wrapper.fmuData.info

import kotlinx.serialization.Serializable

/**
 * Contains metadata information about an FMU (Functional Mock-up Unit).
 *
 * @property modelName The name of the FMU model.
 * @property description A description of the FMU model.
 * @property author The author of the FMU model.
 * @property fmuVersion The version of the FMU standard used.
 * @property defaultExperimentStart The default start time for experiments.
 * @property defaultExperimentStop The default stop time for experiments.
 * @property defaultExperimentStep The default step size for experiments.
 * @property fmuKind The type of FMU (Model Exchange/Co-Simulation).
 * @property variables List of variable names available in the FMU.
 */
@Serializable
data class FmuInfo(
    val modelName: String?,
    val description: String?,
    val author: String?,
    val fmuVersion: String?,
    val defaultExperimentStart: Double = 0.0,
    val defaultExperimentStop: Double = 1.0,
    val defaultExperimentStep: Double = 0.01,
    val fmuKind: String?,
    val variables: Map<String, String>,
    val canSimulate: Boolean,
)
