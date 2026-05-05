package fakes

import preprocessor.FmuPreprocessor

// A no-op preprocessor: returns the input path unchanged.
// Suitable for all native targets — matches the behaviour of
// linuxX64 and mingwX64 actual implementations.
class FakePreprocessor : FmuPreprocessor {
    var lastFmuPath: String? = null
    var lastOutputPath: String? = null

    override fun prepare(fmuPath: String, outputPath: String): String {
        lastFmuPath = fmuPath
        lastOutputPath = outputPath
        return fmuPath
    }
}
