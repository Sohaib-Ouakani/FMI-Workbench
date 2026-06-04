package preprocessor.factory

import logger.FmuKtLogger
import preprocessor.FmuPreprocessor
import recompiler.FmuRecompiler

actual fun createPreprocessor() = object : FmuPreprocessor {
    override fun prepare(fmuPath: String, outputPath: String): String {
        val recompiler = FmuRecompiler()
        val output = "$outputPath/result.fmu"
        recompiler.recompile(fmuPath, output)
        FmuKtLogger.i("Recompilation of FMU model completed. Output at: $output")

        return output
    }
}
