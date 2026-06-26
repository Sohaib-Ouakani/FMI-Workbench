package fakes

import fmu.FmuPaths
import fmu.FmuService
import wrapper.fmuData.info.FmuInfo
import wrapper.simulation.config.SimulationConfig
import wrapper.simulation.results.SimulationResult

class FakeFmuService : FmuService {

    var loadedPaths: FmuPaths? = null
    var closeCalled = false
    var throwOnLoad: Exception? = null
    var throwOnGetInfo: Exception? = null
    var fmuInfoToReturn: FmuInfo = FmuInfo(
        modelName = "FakeModel",
        description = "A fake FMU for testing",
        author = "Test Author",
        fmuVersion = "2.0",
        defaultExperimentStart = 0.0,
        defaultExperimentStop = 1.0,
        defaultExperimentStep = 0.01,
        fmuKind = "Co-Simulation",
        variables = mapOf("variable1" to "kg", "variable2" to "m"),
        canSimulate = true,
    )

    override fun load(paths: FmuPaths) {
        throwOnLoad?.let { throw it }
        loadedPaths = paths
    }

    override fun getInfo(): FmuInfo {
        throwOnGetInfo?.let { throw it }
        return fmuInfoToReturn
    }

    override fun simulate(config: SimulationConfig): SimulationResult = SimulationResult(
        timestamps = listOf(0.0, 0.01),
        variables = mapOf("variable1" to listOf(0.0, 1.0)),
        config = config,
    )

    override fun close() {
        closeCalled = true
    }

    fun reset() {
        loadedPaths = null
        closeCalled = false
        throwOnLoad = null
        throwOnGetInfo = null
    }
}
