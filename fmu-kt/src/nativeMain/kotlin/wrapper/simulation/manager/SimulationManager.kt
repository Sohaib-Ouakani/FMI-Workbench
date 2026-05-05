package wrapper.simulation.manager

import cnames.structs.fmi2_import_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import libfmi.fmi2_import_do_step
import libfmi.fmi2_import_enter_initialization_mode
import libfmi.fmi2_import_exit_initialization_mode
import libfmi.fmi2_import_free_instance
import libfmi.fmi2_import_get_real
import libfmi.fmi2_import_get_variable_by_name
import libfmi.fmi2_import_get_variable_vr
import libfmi.fmi2_import_instantiate
import libfmi.fmi2_import_setup_experiment
import libfmi.fmi2_import_terminate
import libfmi.fmi2_type_t
import libfmi.fmi2_value_reference_tVar
import wrapper.fmuData.infoManager.InfoManagerFmu
import wrapper.fmuLifecycle.FmuLifecycleManager
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

/**
 * Manages FMU simulation execution including setup, initialization, and result collection.
 * Orchestrates the complete simulation workflow from configuration to result retrieval.
 * Handles variable resolution, simulation stepping, and output collection at specified intervals.
 *
 * @property lifecycleManager Reference to the [FmuLifecycleManager] managing the FMU structure.
 * @property infoManagerFmu Reference to the [InfoManagerFmu] for FMU metadata queries.
 */
@OptIn(ExperimentalForeignApi::class)
class SimulationManager(
    private val fmiStruct: CPointer<fmi2_import_t>,
    private val infoManagerFmu: InfoManagerFmu
) {
    /**
     * Indicates whether the simulation has been set up and is ready for execution.
     */
    private var isSimulationSetup: Boolean = false
    /**
     * Current simulation configuration, null if not set.
     */
    var simulationConfig: SimulationConfig? = null
    /**
     * Results from the last executed simulation, null if no simulation has been run.
     */
    var simulationResult: SimulationResult? = null

    /**
     * Sets up the experiment with the given configuration.
     * Configures experiment parameters (tolerance, time bounds),
     * and enters initialization mode. This must be called before [executeSimulation].
     *
     * @param config The [SimulationConfig] containing simulation parameters.
     * @param experimentName The name to assign to the experiment instance. Defaults to "default".
     * @throws IllegalStateException if instantiation or setup fails.
     */
    fun setUpExperiment(config: SimulationConfig, experimentName: String = "default") {
        if (!isSimulationSetup) instantiate(config.experimentName)

        simulationConfig = config

        fmi2_import_setup_experiment(
            fmiStruct,
            if (config.tolerance != null) 1 else 0,
            config.tolerance ?: 0.0,
            config.startTime,
            if (config.stopTime != null) 1 else 0,
            config.stopTime ?: 0.0
        )

        fmi2_import_enter_initialization_mode(fmiStruct)
        fmi2_import_exit_initialization_mode(fmiStruct)
    }

    /**
     * Executes the configured simulation and collects results.
     * Runs time-stepping simulation from start to stop time, recording variable values at each step.
     * Variables are determined by the simulation configuration; if none specified, all FMU variables are recorded.
     *
     * @return [SimulationResult] containing timestamps and variable value history.
     * @throws IllegalStateException if the simulation has not been set up or config is missing.
     */
    fun executeSimulation(): SimulationResult {
        if (!isSimulationSetup) {
            throw IllegalStateException("Simulation not set up.")
        }
        if (simulationConfig == null) {
            throw IllegalStateException("Simulation config is null.")
        }

        val config = simulationConfig!!
        val fmuInfo = infoManagerFmu.extractFmuInfo()

        val stopTime: Double = config.stopTime
            ?: fmuInfo.defaultExperimentStop
        val step: Double = config.stepSize
        var time = config.startTime

        // Determina quali variabili leggere
        val variablesToRead = config.outputVariables.ifEmpty {
            fmuInfo.variables  // tutte le variabili dell'FMU
        }

        val timestamps = mutableListOf<Double>()

        // Una lista per ogni variabile
        val results = variablesToRead.associateWith { mutableListOf<Double>() }

        try {
            memScoped {
                // Risolvi i value reference per ogni variabile
                val vrMap = variablesToRead.associateWith { varName ->
                    val variable = fmi2_import_get_variable_by_name(fmiStruct, varName)
                        ?: error("Variabile '$varName' non trovata nell'FMU")
                    fmi2_import_get_variable_vr(variable)
                }

                val n = variablesToRead.size
                val vrArray = allocArray<fmi2_value_reference_tVar>(n)
                val valueArray = allocArray<DoubleVar>(n)

                variablesToRead.forEachIndexed { i, varName ->
                    vrArray[i] = vrMap[varName]!!
                }


                println("---- Simulation Start ----")

                while (time < stopTime) {
                    val remaining = stopTime - time
                    if (remaining <= 0.0) break
                    val actualStep = minOf(step, remaining)

                    fmi2_import_do_step(
                        fmiStruct,
                        time,
                        actualStep,
                        1
                    )

                    fmi2_import_get_real(
                        fmiStruct,
                        vrArray,
                        n.toULong(),
                        valueArray
                    )

                    timestamps.add(time)
                    variablesToRead.forEachIndexed { i, varName ->
                        results[varName]!!.add(valueArray[i])
                    }

                    time += actualStep
                }

                println("---- Simulation End ----")
            }
            val results = SimulationResult(
                timestamps = timestamps,
                variables = results,
                config = config
            )
            simulationResult = results

            return results
        } finally {
            try {
                fmi2_import_terminate(fmiStruct)
                fmi2_import_free_instance(fmiStruct)
            } catch (e: Exception) {
                throw IllegalStateException("Error during simulation cleanup: ${e.message}", e)
            }
            isSimulationSetup = false
            simulationConfig = null
        }
    }

    /**
     * Instantiates an FMU instance for simulation.
     * Creates a new FMU component ready for configuration and execution.
     *
     * @param experimentName The name to assign to the experiment instance. Defaults to "default".
     * @throws IllegalStateException if the experiment is already instantiated or instantiation fails.
     */
    private fun instantiate(experimentName: String = "default") {
        if(isSimulationSetup) {
            throw IllegalStateException("Already instanced simulation")
        }
        val status = fmi2_import_instantiate(
            fmiStruct,
            experimentName,
            fmi2_type_t.fmi2_cosimulation,
            null,
            0
        )

        if (status != 0) {
            throw IllegalStateException("Failed to instantiate the simulation")
        }

        isSimulationSetup = true
    }
}
