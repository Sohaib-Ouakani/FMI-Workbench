package wrapper

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import preprocessor.FmuPreprocessor
import wrapper.fmuData.info.FmuInfo
import wrapper.fmuData.infoManager.InfoManagerFmu
import wrapper.fmuLifecycle.FmuLifecycleManager
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.manager.SimulationManager
import wrapper.simulation.results.SimulationResult

/**
 * Low-level wrapper for native FMI (Functional Mock-up Interface) operations.
 * Provides direct access to FMILib functions for loading, configuring, and running FMU simulations.
 * Handles platform-specific preprocessing and delegates simulation management to specialized managers.
 *
 * This class orchestrates the FMU lifecycle through three main managers:
 * - [FmuLifecycleManager]: Handles FMU loading and resource management
 * - [InfoManagerFmu]: Extracts and manages FMU metadata
 * - [SimulationManager]: Manages simulation execution and results
 *
 * @property path Path to the FMU file to be loaded.
 * @property extractedDir Path to the resource directory containing FMU contents.
 * @property modelsDir Directory where compiled and preprocessed models are stored.
 * @property fmuLifecycle Manager for FMU loading, parsing, and cleanup.
 * @property infoFmu Manager for extracting FMU metadata and variables.
 * @property simulationManager Manager for simulation setup, execution, and results collection.
 * @throws IllegalStateException if FMU binary loading fails or XML parsing is unsuccessful.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
class NativeFmiWrapper(
    val path: String,
    val extractedDir: String,
    val modelsDir: String,
    preprocessor: FmuPreprocessor
) : AutoCloseable {

    private val fmuLifecycle: FmuLifecycleManager = FmuLifecycleManager(
        preprocessor.prepare(path, modelsDir),
        extractedDir
    )
    private val infoFmu: InfoManagerFmu
    private val simulationManager: SimulationManager
    init {
        fmuLifecycle.start()
        val struct = fmuLifecycle.requireFmiStruct()
        infoFmu = InfoManagerFmu(struct)
        simulationManager = SimulationManager(struct, infoFmu)
    }

    /**
     * Extracts and returns the FMU metadata.
     * Parses FMU information including model name, description, author, version, default experiment
     * parameters, FMU kind (Model Exchange, Co-Simulation, or both), and available variables.
     *
     * @return [FmuInfo] containing the complete FMU metadata.
     * @throws IllegalStateException if the FMU has not been loaded or XML parsing failed.
     */
    fun getInfo(): FmuInfo {
        return infoFmu.extractFmuInfo()
    }

    /**
     * Sets up the experiment with the given simulation configuration.
     * Configures the FMU for simulation including tolerance, start/stop times, and enters initialization mode.
     * If no experiment instance exists, one will be created automatically with the name from the configuration.
     *
     * @param config The [SimulationConfig] containing parameters like start time, stop time, step size, tolerance,
     * and output variables.
     * @throws IllegalStateException if the FMU is not loaded or configuration is invalid.
     */
    fun setupExperiment(config: SimulationConfig) {
        simulationManager.setUpExperiment(config)
    }

    /**
     * Executes the simulation experiment and returns the results.
     * Runs the simulation from start time to stop time, recording variable values at each time step.
     * Uses the variables specified in the simulation config. If none are specified, all FMU variables are recorded.
     *
     * @return A [SimulationResult] containing timestamps and variable values from the simulation.
     * @throws IllegalStateException if the experiment is not set up or simulation config is not initialized.
     */
    fun executeExperiment(): SimulationResult {
        return simulationManager.executeSimulation()
    }

    /**
     * Closes the FMU wrapper and releases all associated native resources.
     * Internally delegates to [FmuLifecycleManager.close].
     * Should be called when done with the FMU wrapper to prevent resource leaks.
     * Can be used automatically with try-with-resources statements due to [AutoCloseable] implementation.
     *
     * @throws IllegalStateException if an error occurs during FMU cleanup.
     */
    override fun close() {
        fmuLifecycle.close()
    }
}
