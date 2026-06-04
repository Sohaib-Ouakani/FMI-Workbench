package utility

/**
 * Manages FMU (Functional Mock-up Unit) file packaging operations.
 * Handles extraction of FMU contents, creation of temporary directories, and repackaging.
 *
 * @param exec The [ProcessExecution] instance used to run system commands like unzip and zip.
 */
class FmuPackager(
    private val exec: ProcessExecution
) {
    /**
     * Extracts the contents of an FMU file to a specified destination directory.
     *
     * @param fmuPath The path to the FMU file to extract.
     * @param destDir The destination directory where contents will be extracted.
     * @throws IllegalStateException if the unzip command fails.
     */
    fun extract(fmuPath: String, destDir: String) {
        exec.run("mkdir", "-p", destDir)
        check(exec.run("unzip", "-q", fmuPath, "-d", destDir) == 0) {
            "Extraction failed: $fmuPath"
        }
    }

    /**
     * Creates a ZIP archive from a source directory to produce an FMU file.
     * The FMU file is created with all contents of the source directory.
     *
     * @param sourceDir The directory containing the FMU contents to package.
     * @param outputFmu The path where the output FMU (ZIP) file will be created.
     * @throws IllegalStateException if the zip fails.
     */
    fun zip(sourceDir: String, outputFmu: String) {
        check(exec.run("cd", "\"$sourceDir\"", "&&", "zip", "-qr", "\"$outputFmu\"", ".") == 0) {
            "zip failed"
        }
    }

    /**
     * Creates a temporary directory, executes a block of code that may use the temporary directory,
     * and cleans it up afterward.
     * This is a resource management pattern to ensure temporary directories are removed even if an error occurs.
     *
     * @param block A lambda function that receives the temporary directory path and may perform operations within it.
     * @return The result of executing the lambda block.
     * @throws IllegalStateException if temporary directory creation fails.
     */
    fun <T> withTempDir(block: (String) -> T): T {
        val tmp = exec.runWithOutput("mktemp", "-d", "/tmp/fmu_recompile_XXXXXX")
            .also { check(it.isNotBlank()) { "Temp folder creation failed" } }
        return try {
            block(tmp)
        } finally {
            exec.run("rm", "-rf", tmp)
        }
    }
}
