package utility

/**
 * Manages the compilation and linking of C source files using Clang.
 * Handles compilation to object files, linking into dynamic libraries,
 * and creation of universal binaries for multiple architectures.
 *
 * @param exec The [ProcessExecution] instance used to run Clang commands.
 */
class ClangComplier(
    private val exec: ProcessExecution
) {
    /**
     * Compiles C source files to object files for a specific target architecture.
     *
     * @param sources List of absolute paths to C source files to compile.
     * @param target The Clang target triple (e.g., "arm64-apple-macos11").
     * @param includeDir The directory containing header files for compilation.
     * @param outDir The output directory where object files will be placed.
     * @return A list of paths to the generated object files.
     * @throws IllegalStateException if compilation fails for any source file.
     */
    fun compileObjects(
        sources: List<String>,
        target: String,
        includeDir: String,
        outDir: String
    ): List<String> {
        exec.run("mkdir", "-p", outDir)
        return sources.map { src ->
            val obj = "$outDir/${src.substringAfterLast('/').removeSuffix(".c")}.o"
            check(
                exec.run(
                    "clang",
                    "--target=$target",
                    "-c", "-fPIC", "-O2",
                    "-I$includeDir",
                    src, "-o", obj
                ) == 0
            ) { "Compilation failed: $src [${target.substringBefore("-")}]" }
            obj
        }
    }

    /**
     * Links object files into a dynamic library for a specific target architecture.
     *
     * @param objs List of object file paths to link.
     * @param target The Clang target triple (e.g., "arm64-apple-macos11").
     * @param aliasFlags Alias flags for symbol aliasing during linkage.
     * @param out The output path for the generated dynamic library.
     * @throws IllegalStateException if linking fails.
     */
    fun linkDylib(
        objs: List<String>,
        target: String,
        aliasFlags: List<String>,
        out: String
    ) {
        check(
            exec.run(
                "clang",
                "--target=$target",
                "-dynamiclib", "-fPIC",
                *aliasFlags.toTypedArray(),
                *objs.toTypedArray(),
                "-lm",
                "-Wl,-undefined,dynamic_lookup",
                "-o", out
            ) == 0
        ) { "Link failed [${target.substringBefore("-")}]" }
    }

    /**
     * Creates a universal (multi-architecture) binary from architecture-specific dynamic library slices.
     *
     * @param slices List of dynamic library paths, each compiled for a different architecture.
     * @param out The output path for the generated universal binary.
     * @throws IllegalStateException if the command for generating universal binary fails.
     */
    fun createUniversalBinary(slices:  List<String>, out: String) {
        check(exec.run("lipo", "-create", *slices.toTypedArray(), "-output", out) == 0) {
            "generation of of universal binary failed: lipo failed"
        }
    }

    /**
     * Discovers symbol aliases from compiled object files for the given model identifier and FMI prefix.
     * Generates linker flags needed to create aliases between full and shortened symbol names.
     *
     * @param objs List of compiled object file paths to scan for symbols.
     * @param modelId The model identifier used to find relevant symbols.
     * @param fmiPrefix The FMI prefix (e.g., "fmi2") to filter relevant symbols.
     * @return A list of linker flags for creating symbol aliases.
     */
    fun discoverAliasFlags(objs: List<String>, modelId: String, fmiPrefix: String): List<String> =
        objs.flatMap { obj ->
            exec.runWithOutput("nm", "--defined-only", obj)
                .lines()
                .mapNotNull {
                    Regex("""[_]?(${modelId}_${fmiPrefix}[A-Za-z]+)""")
                        .find(it)?.groupValues?.get(1)
                }
                .toSet()
                .map { raw ->
                    val suffix = raw.removePrefix("${modelId}_")
                    "-Wl,-alias,_$raw,_$suffix"
                }
        }.distinct()
}
