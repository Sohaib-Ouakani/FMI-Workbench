package wrapper.simulation.config

import kotlinx.serialization.Serializable

/**
 * Configuration parameters for running an FMU simulation.
 * Defines the time parameters, numerical settings, and output specifications for the simulation.
 *
 * @property startTime The time at which the simulation should start. Defaults to 0.0.
 * @property stopTime The time at which the simulation should stop. If null, uses FMU default.
 * @property stepSize The time step size for the simulation. Defaults to 0.01.
 * @property tolerance The numerical tolerance for the simulation. If null, uses FMU default.
 * @property experimentName A name for the experiment instance. Defaults to "Default".
 * @property outputVariables List of variable names to record during simulation. If empty, records all variables.
 */
@Serializable
data class SimulationConfig(
    val startTime: Double = 0.0,
    val stopTime: Double? = null,
    val stepSize: Double = 0.01,
    val tolerance: Double? = null,
    val experimentName: String = "Default",
    val outputVariables: List<String> = emptyList(),
)
