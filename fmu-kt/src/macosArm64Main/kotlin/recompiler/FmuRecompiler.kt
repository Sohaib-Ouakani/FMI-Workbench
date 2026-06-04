package recompiler

import logger.FmuKtLogger
import platform.posix.F_OK
import platform.posix.access
import utility.ClangComplier
import utility.DescriptionParser
import utility.FilesystemManager
import utility.FmiHeaderSynthesiser
import utility.FmuPackager
import utility.ProcessExecution

/**
 * Recompiles FMU (Functional Mock-up Unit) files for the macOS ARM64 platform.
 * This class handles the extraction of FMU contents, compilation of source files,
 * linking into universal binaries supporting multiple architectures, and repackaging.
 */
class FmuRecompiler {
    private val exec = ProcessExecution()
    private val fs = FilesystemManager()
    private val packager = FmuPackager(exec)
    private val complier = ClangComplier(exec)
    private val headers = FmiHeaderSynthesiser(fs)

    private val fmiPrefix = "fmi2"
    private val targets = listOf("arm64-apple-macos11", "x86_64-apple-macos10.13")

    /**
     * Recompiles the given input FMU file and produces a new FMU at the output path.
     * The process involves extracting the FMU, synthesizing necessary headers,
     * compiling source files for specified targets, linking into a universal binary,
     * and repackaging the modified FMU.
     *
     * @param inputFmu The file path to the input FMU file.
     * @param outputFmu The file path where the recompiled FMU will be saved.
     * @throws IllegalStateException if the input FMU does not exist or lacks source files.
     */
     fun recompile(inputFmu: String, outputFmu: String) {
        val input = fs.pathAbsolute(inputFmu)
        val output = fs.pathAbsolute(outputFmu)
        if(!fs.fileExists(input)) {
            throw IllegalArgumentException("FMU not found: $input")
        }

        packager.withTempDir { tmp ->
            val extracted = "$tmp/extracted"
            packager.extract(input, extracted)

            val xml = fs.readFile("$extracted/modelDescription.xml")
            val parser = DescriptionParser(xml)
            val modelId  = parser.findModelId()
            val sourcesDir = "$extracted/sources"

            check(access(sourcesDir, F_OK) == 0) { "No source folder, precompiled FMU" }

            headers.synthesise(sourcesDir)

            val sources = parser.findSourceFiles().map { "$sourcesDir/$it" }

            val objsByArch = targets.associateWith { target ->
                val arch = target.substringBefore("-")
                complier.compileObjects(
                    sources = sources,
                    target = target,
                    includeDir = sourcesDir,
                    outDir = "$tmp/obj_$arch"
                )
            }

            // Alias of symbols of arm64 are the same of x86_64, that's why .first()
            val aliasFlags = complier.discoverAliasFlags(
                objs = objsByArch.values.first(),
                modelId = modelId,
                fmiPrefix = fmiPrefix
            )

            val slices = targets.map { target ->
                val arch  = target.substringBefore("-")
                val slice = "$tmp/$arch.dylib"

                complier.linkDylib(
                    objs = objsByArch[target]!!,
                    target = target,
                    aliasFlags = aliasFlags,
                    out = slice
                )
                slice
            }

            val universal = "$tmp/$modelId.dylib"
            complier.createUniversalBinary(
                slices = slices,
                out = universal
            )

            val binDest = "$extracted/binaries/darwin64"
            exec.run("mkdir", "-p", binDest)
            FmuKtLogger.d("modelId at cp step: '$modelId'")
            exec.run("cp", universal, "$binDest/$modelId.dylib")

            FmuKtLogger.d("binary at: $binDest/$modelId.dylib exists=${fs.fileExists("$binDest/$modelId.dylib")}")
            packager.zip(extracted, output)
        }
    }

}
