package fmu

import preprocessor.FmuPreprocessor
import preprocessor.factory.createPreprocessor
import wrapper.NativeFmiWrapper
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

/**
 * High-level wrapper for FMU (Functional Mock-up Unit) operations.
 * Provides a simplified interface for loading, configuring, running, and managing FMU simulations.
 * Implements [AutoCloseable] to ensure proper resource cleanup.
 *
 * This class acts as the main entry point for FMU interactions, hiding complexity of the low-level
 * [NativeFmiWrapper] and providing a clean API for users.
 *
 * @property fmuPath Path to the FMU file to be loaded.
 * @property resourcesPath Path to the resources directory containing extracted FMU contents.
 * @property modelsDir Directory where compiled models are stored.
 * @property fmuInfo FMU metadata including model name, description, and available variables.
 * @throws IllegalArgumentException if the FMU cannot be loaded or is corrupted.
 */
class DefaultFmu(
    private val preprocessor: FmuPreprocessor
) : FmuService {
    private var wrapper: NativeFmiWrapper? = null

    /**
     * Closes the FMU and releases all associated resources.
     * This method is called automatically when using try-with-resources or when the object is garbage collected.
     * Safe to call multiple times.
     */
    override fun close() {
        wrapper?.close()
        wrapper = null
    }

    override fun load(paths: FmuPaths) {
        close()
        wrapper = NativeFmiWrapper(
            paths.fmuPath,
            paths.extractedDir,
            paths.modelsDir,
            preprocessor
        )
    }

    override fun getInfo(): FmuInfo {
        return wrapper?.getInfo() ?: throw IllegalStateException("Cannot get info: FMU not loaded")
    }

    override fun simulate(config: SimulationConfig): SimulationResult {
        val fmi = wrapper ?: throw IllegalStateException("Cannot simulate: FMU not loaded")
        fmi.setupExperiment(config)
        return fmi.executeExperiment()
    }
}
