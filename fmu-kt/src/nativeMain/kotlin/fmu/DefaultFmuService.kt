package fmu

import preprocessor.factory.createPreprocessor
import wrapper.FmuManager
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

class DefaultFmuService : FmuService {
    private var wrapper: FmuManager? = null
    private var loadedFmuPath: String? = null

    /**
     * Closes the FMU and releases all associated resources.
     * This method is called automatically when using try-with-resources or when the object is garbage collected.
     * Safe to call multiple times.
     */
    override fun close() {
        wrapper?.close()
        wrapper = null
        loadedFmuPath = null
    }

    override fun load(paths: FmuPaths) {
        if (paths.fmuPath == loadedFmuPath && wrapper != null) return
        close()
        loadedFmuPath = paths.fmuPath
        wrapper = FmuManager(
            paths.fmuPath,
            paths.extractedDir,
            paths.modelsDir,
            createPreprocessor(),
        )
    }

    override fun getInfo(): FmuInfo =
        wrapper?.getInfo() ?: throw IllegalStateException("Cannot get info: FMU not loaded")

    override fun simulate(config: SimulationConfig): SimulationResult {
        val fmi = wrapper ?: throw IllegalStateException("Cannot simulate: FMU not loaded")
        fmi.setupExperiment(config)
        return fmi.executeExperiment()
    }
}
