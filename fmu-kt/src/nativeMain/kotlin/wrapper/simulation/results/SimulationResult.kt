package wrapper.simulation.results

import kotlinx.serialization.Serializable
import wrapper.simulation.config.SimulationConfig

/**
 * Represents the results of a simulation run.
 * Contains timestamps, variable values over time, and the configuration used for the simulation.
 *
 * @property timestamps List of time points at which the simulation state was recorded.
 * @property variables Map of variable names to their values over time.
 * Each variable has a list of values corresponding to the timestamps.
 * @property config The [SimulationConfig] used to run the simulation.
 */
@Serializable
data class SimulationResult(
    val timestamps: List<Double>,
    val variables: Map<String, List<Double>>,
    val config: SimulationConfig,
)
