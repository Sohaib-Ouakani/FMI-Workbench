package preprocessor

interface FmuPreprocessor {
    fun prepare(fmuPath: String, outputPath: String): String
}
