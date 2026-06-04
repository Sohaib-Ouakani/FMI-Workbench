package preprocessor.factory

import preprocessor.FmuPreprocessor

actual fun createPreprocessor() = object : FmuPreprocessor {
    override fun prepare(fmuPath: String, outputPath: String) = fmuPath  // no-op
}
